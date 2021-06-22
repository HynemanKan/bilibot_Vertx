package per.hynemankan.vertx.bilibot.db;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.client.*;
import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import per.hynemankan.vertx.bilibot.utils.EventBusChannels;
import per.hynemankan.vertx.bilibot.handlers.common.HealthChecker;
import per.hynemankan.vertx.bilibot.utils.ConfigurationKeys;

@Slf4j
public class RedisUtils extends AbstractVerticle {

  /**
   * 同步锁最大重试次数
   */
  private static final int RETRY_TIMES = 50;
  /**
   * 同步锁重试固定间隔基数
   */
  private static final int RETRY_DELAY = 70;
  /**
   * 同步锁重试随机时间范围
   */
  private static final int RETRY_DELAY_SCOPE = 30;
  /**
   * 锁过期时间单位 EX:秒 PX:毫秒
   */
  private static final String TIMEOUT_UNIT = "EX";
  /**
   * 超时时长
   */
  private static final int EXPIRE_TIME = 1;
  /**
   * redis连接客户端
   */
  private static RedisConnection client;
  /**
   * 连接字串（地址端口用户名密码）
   */
  private String connectString;
  /**
   * redis密码
   */
  private String password;
  /**
   * 连接池大小
   */
  private int maxPoolSize;
  /**
   * 最大等待数量
   */
  private int maxWaitingHandlers;
  /**
   * redis 配置
   */
  private RedisOptions options;

  public static RedisConnection getClient() {
    return client;
  }

  private static void setClient(RedisConnection client) {
    RedisUtils.client = client;
  }

  @Override
  public void start(Promise<Void> startPromise) {
    vertx.eventBus().<JsonObject>consumer(EventBusChannels.SET_REDIS_OPTIONS.name()).handler(this::setRedisOptions);
    vertx.eventBus().<String>consumer(EventBusChannels.REDIS_CONNECT.name()).handler(this::initRedis);
    vertx.eventBus().<JsonObject>consumer(EventBusChannels.GET_DISTRIBUTED_LOCK.name()).handler(this::getDistributedLock);
    vertx.eventBus().<JsonObject>consumer(EventBusChannels.RELEASE_DISTRIBUTED_LOCK.name()).handler(this::releaseDistributedLock);
    vertx.eventBus().request(EventBusChannels.SET_REDIS_OPTIONS.name(), config().getJsonObject("redis"), r -> {
      if (r.succeeded()) {
        startPromise.complete();
      } else {
        startPromise.fail("Redis init failed!");
      }
    });
  }

  /**
   * redis初始化
   */
  private void initRedis(Message<String> message) {
    options = new RedisOptions();
    options.setConnectionString(connectString)
      .setMaxPoolWaiting(maxWaitingHandlers)
      .setMaxPoolSize(maxPoolSize)
      .setMaxWaitingHandlers(maxWaitingHandlers);
    createRedisClient(onCreate -> {
      if (onCreate.succeeded()) {
        // connected to redis!
        log.info("Redis connected!");
        message.reply(true);
      } else {
        message.reply(false);
        throw new RuntimeException("Redis failed!", onCreate.cause());
      }
    });
  }

  /**
   * 创建redis连接及设置错误处理
   */
  private void createRedisClient(Handler<AsyncResult<RedisConnection>> handler) {
    Redis.createClient(vertx, options)
      .connect(onConnect -> {
        if (onConnect.succeeded()) {
          setClient(onConnect.result());
          RedisAPI.api(client).auth(Collections.singletonList(password), ar -> {
          });
          HealthChecker.setRedisHealthy(HealthChecker.STATUS_WORKING);
          client.exceptionHandler(e -> {
            log.warn("Redis encounters an error", e);
          });
        } else {
          log.warn(onConnect.cause().getMessage());
        }
        handler.handle(onConnect);
      });
  }

  /***
   * 获取分布式锁
   * @param task   eventbus message:锁，请求编号，锁过期时间单位（可选，默认秒），锁过期时间（默认1）
   */
  public void getDistributedLock(Message<JsonObject> task) {
    //EX:秒 PX:毫秒
    String timeUnit;
    //锁过期时间
    int expireTime;
    String lockKey = task.body().getString("lock");
    String requestId = task.body().getString("requestId");
    if (task.body().containsKey("timeUnit")) {
      timeUnit = task.body().getString("timeUnit");
    } else {
      timeUnit = TIMEOUT_UNIT;
    }
    if (task.body().containsKey("expireTime")) {
      expireTime = task.body().getInteger("expireTime");
    } else {
      expireTime = EXPIRE_TIME;
    }

    tryGetDistributedLock(lockKey, requestId, timeUnit, expireTime, rs -> {
      if (Boolean.TRUE.equals(rs)) {
        task.reply("Get dLock success");
      } else {
        // 获取失败，进入等待自旋，尝试N次后失败丢弃。
        waitLock(task, timeUnit, expireTime, lockKey, requestId);
      }
    });
  }

  /***
   * 自旋等待获取锁
   * @param task eventbus message
   * @param timeUnit 过期时间单位
   * @param expireTime 过期时间
   * @param lockKey 锁名
   * @param requestId 锁标识
   */
  private void waitLock(Message<JsonObject> task, String timeUnit, int expireTime, String lockKey, String requestId) {
    AtomicInteger waitTimes = new AtomicInteger(1);
    vertx.setPeriodic((long) new SecureRandom().nextInt(RETRY_DELAY_SCOPE) + RETRY_DELAY, ph ->
      tryGetDistributedLock(lockKey, requestId, timeUnit, expireTime, rs -> {
        if (Boolean.TRUE.equals(rs)) {
          vertx.cancelTimer(ph);
          task.reply("Get dLock success");
        } else {
          if (waitTimes.addAndGet(1) > RETRY_TIMES) {
            vertx.cancelTimer(ph);
            log.warn("Get lock failed!");
            task.fail(0, "Get dLock failed");
          }
        }
      })
    );
  }


  /***
   * 获取分布式锁
   * @param lockKey 锁名称
   * @param requestId 锁标识
   * @param timeUnit 过期时间单位
   * @param expireTime 过期时间
   */
  private void tryGetDistributedLock(String lockKey, String requestId,
                                     String timeUnit, int expireTime, Handler<Boolean> handler) {

    List<String> list = Arrays.asList(lockKey, requestId, timeUnit, String.valueOf(expireTime), "NX");
    RedisAPI.api(client).set(list, h -> handler.handle(h.result() != null));

  }

  /**
   * 释放分布式锁
   *
   * @param task eventbus message 包括：锁，请求编号
   */

  public void releaseDistributedLock(Message<JsonObject> task) {
    String lockKey = task.body().getString("lock");
    String requestId = task.body().getString("requestId");
    String script =
      "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
    RedisAPI.api(client).eval(Arrays.asList(script, "1", lockKey, requestId), h -> {
      if (h.succeeded() && h.result().toInteger() == 1) {
        task.reply("Release dLock success");
      } else {
        task.fail(0, "Release dLock failed");
      }
    });
  }

  /***
   *  redis配置变更
   * @param message connectString，maxPoolSize，maxWaitingHandlers配置信息
   */
  private void setRedisOptions(Message<JsonObject> message) {
    String newConnectString = message.body().getString(ConfigurationKeys.CONNECT_STRING.name());
    int newMaxPoolSize = message.body().getInteger(ConfigurationKeys.MAX_POOL_SIZE.name());
    int newMaxWaitingHandlers = message.body().getInteger(ConfigurationKeys.MAX_WAITING_HANDLERS.name());
    String newPassword = message.body().getString(ConfigurationKeys.PASSWORD.name());
    // 不涉及redis配置变更直接返回
    if (!newConnectString.equals(this.connectString)
      || this.maxPoolSize != newMaxPoolSize
      || this.maxWaitingHandlers != newMaxWaitingHandlers
      || !this.password.equals(newPassword)
    ) {

      this.connectString = newConnectString;
      this.maxPoolSize = newMaxPoolSize;
      this.maxWaitingHandlers = newMaxWaitingHandlers;
      this.password = newPassword;


      vertx.eventBus().request(EventBusChannels.REDIS_CONNECT.name(), message.body(),
        r -> {
          if (r.succeeded()) {
            message.reply(true);
          } else {
            log.warn("Redis connect failed!", r.cause());
            message.fail(-1, "Redis connect failed!");
          }
        }
      );
    } else {
      message.reply(true);
    }
  }
}
