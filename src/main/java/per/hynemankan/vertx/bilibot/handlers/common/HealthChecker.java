package per.hynemankan.vertx.bilibot.handlers.common;

//import io.vertx.core.CompositeFuture;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
//import io.vertx.core.json.JsonObject;
import io.vertx.redis.client.RedisAPI;

import java.util.Collections;

import lombok.extern.slf4j.Slf4j;
import per.hynemankan.vertx.bilibot.db.MysqlUtils;
import per.hynemankan.vertx.bilibot.db.RedisUtils;

/***
 * 健康检查
 * redisHealthy 状态：working,failed
 */
@Slf4j
public class HealthChecker {
  /**
   * redis正常状态
   */
  public static final String STATUS_WORKING = "working";
  /**
   * redis 异常状态
   */
  public static final String STATUS_FAILED = "failed";
  /**
   * redis0 log
   */
  private static final String LOG_REDIS_STATUS_FAILED = "Redis connect failed";
  /**
   * Mysql log
   */
  private static final String LOG_MYSQL_STATUS_FAILED = "Mysql connect failed";
  /**
   * 健康检查状态
   */
  private static boolean healthy = false;
  /**
   * redis状态
   */
  private static String redisHealthy = STATUS_FAILED;
  /**
   * mysql状态
   */
  private static String mysqlHealthy = STATUS_FAILED;

  /**
   * Singleton Pattern
   */
  private HealthChecker() {
    throw new IllegalStateException("Utility class");
  }

  public static boolean isHealthy() {
    return healthy;
  }

  private static void setHealthy(boolean healthyStatus) {
    healthy = healthyStatus;
  }

  public static Future<Void> checkHealthy(Vertx vertx) {
    return Future.future(result -> {
      if (healthy) {
        // redis 健康测试
        Future<String> redisFuture = checkRedis();
        Future<String> mysqlFuture = checkMysql();
        // mysql 健康测试
        CompositeFuture.all(redisFuture, mysqlFuture).onSuccess(r -> {
          healthy = true;
          log.debug("Healthy check success!");
          result.complete();
        }).onFailure(f -> {
          healthy = false;
          result.fail(f.getCause());
        });
      } else {
        if (STATUS_FAILED.equals(redisHealthy)) {
          log.warn("redis Fail");
        }
        if (STATUS_FAILED.equals(mysqlHealthy)) {
          log.warn("mysql Fail");
        }

        log.warn("already Unhealth,skip health check");
      }
    });
  }


  /**
   * 更新redis状态及健康检查状态
   */
  public static void setRedisHealthy(String redisHealthyStatus) {
    redisHealthy = redisHealthyStatus;
    setHealthy(STATUS_WORKING.equals(redisHealthyStatus));
  }

  /**
   * 更新mysql状态及健康检查状态
   */
  public static void setMysqlHealthy(String mysqlHealthyStatus) {
    mysqlHealthy = mysqlHealthyStatus;
    setHealthy(STATUS_WORKING.equals(mysqlHealthyStatus));
  }

  private static Future<String> checkRedis() {
    return Future.future(result ->
      RedisAPI.api(RedisUtils.getClient()).ping(Collections.singletonList("Healthy check"), send -> {
        if (send.succeeded()) {
          setRedisHealthy(STATUS_WORKING);
          log.debug("Healthy check success!");
          result.complete();
        } else {
          log.error(LOG_REDIS_STATUS_FAILED);
          result.fail(LOG_REDIS_STATUS_FAILED);
        }
      }));
  }

  private static Future<String> checkMysql() {

    return Future.future(result ->

      MysqlUtils.getMysqlClient()
        .query("select 1")
        .execute(lm -> {
          if (lm.succeeded()) {
            setMysqlHealthy(STATUS_WORKING);
            result.complete();
          } else {
            log.error(LOG_MYSQL_STATUS_FAILED);
            result.fail(LOG_MYSQL_STATUS_FAILED);
          }
        }));
  }

}
