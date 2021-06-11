package per.hynemankan.vertx.bilibot.handlers.common;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import per.hynemankan.vertx.bilibot.expection.RedisAPIException;
import per.hynemankan.vertx.bilibot.utils.EventBusChannels;

import static io.vertx.core.http.HttpMethod.LOCK;
@Slf4j
public class RedisLockHandler {
  private static Vertx vertx;

  public static void init(Vertx vertx) {
    RedisLockHandler.vertx = vertx;
  }

  /**
   * 获取分布式锁
   *
   * @param requestId 锁编号
   * @return future成功失败信息
   */
  public static Future<Void> getDistributionLock(String lockName,String requestId) {
    JsonObject data = new JsonObject()
      .put("lock", lockName)
      .put("requestId", requestId);
    return Future.future(result -> vertx.eventBus().request(
      EventBusChannels.GET_DISTRIBUTED_LOCK.name(), data, rs -> {
        if (rs.succeeded()) {
          result.complete();
        } else {
          log.warn("Get distributed lock failed! requestId:{}", requestId);
          result.fail(new RedisAPIException("Get distributed lock failed!", rs.cause()));
        }
      }));
  }

  /**
   * 释放分布式锁
   *
   * @param requestId 锁编号
   * @return future成功失败信息
   */
  public static Future<Void> releaseDistributionLock(String lockName,String requestId) {
    JsonObject data = new JsonObject()
      .put("lock", lockName)
      .put("requestId", requestId);
    return Future.future(result -> vertx.eventBus().request(
      EventBusChannels.RELEASE_DISTRIBUTED_LOCK.name(), data, rs -> {
        if (rs.succeeded()) {
          result.complete();
        } else {
          log.warn("Release distributed lock failed! requestId:{}", requestId);
          result.fail(new RedisAPIException("Release distributed lock failed!", rs.cause()));
        }
      }));
  }
}
