package per.hynemankan.vertx.bilibot.utils;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.redis.client.RedisAPI;
import lombok.extern.slf4j.Slf4j;
import per.hynemankan.vertx.bilibot.expection.UnhealthyException;
import per.hynemankan.vertx.bilibot.expection.UnloginException;
import per.hynemankan.vertx.bilibot.handlers.common.HealthChecker;
import per.hynemankan.vertx.bilibot.handlers.common.RedisLockHandler;

import java.util.*;

import static per.hynemankan.vertx.bilibot.db.RedisUtils.getClient;

/**
 * cookies管理器 带并发锁
 *
 * @author hyneman
 */
@Slf4j
public class CookiesManager {
  public static Future<Void> updateCookiesFormHttpBody(HttpResponse<Buffer> httpbody) {
    return Future.future(response -> {
      if (!HealthChecker.isHealthy()) {
        response.fail(new UnhealthyException());
      }
      JsonObject cookies = cookiesDecode(httpbody.cookies());
      log.info(cookies.toString());
      RedisAPI.api(getClient()).exists(Collections.singletonList(GlobalConstants.RD_LOGIN_COOKIES))
        .onSuccess(res -> {
          Boolean isExists = res.toBoolean();
          if (!isExists) {
            String redisOperationId = UUID.randomUUID().toString();
            RedisLockHandler.getDistributionLock(GlobalConstants.COOKIES_LOCK, redisOperationId)
              .onFailure(rs -> response.fail(rs)).onSuccess(rs -> {
              List<String> list = Arrays.asList(
                GlobalConstants.RD_LOGIN_COOKIES,
                cookies.toString(),
                GlobalConstants.TIME_S_MARK,
                String.valueOf(GlobalConstants.RD_LOGIN_COOKIES_TIMEOUT));
              RedisAPI.api(getClient()).set(list, ar -> {
                RedisLockHandler.releaseDistributionLock(GlobalConstants.COOKIES_LOCK, redisOperationId)
                  .onSuccess(re -> {
                  }).onFailure(re -> response.fail(re));
                response.complete();
              });
            });
          } else {
            String redisOperationId = UUID.randomUUID().toString();
            RedisLockHandler.getDistributionLock(GlobalConstants.COOKIES_LOCK, redisOperationId)
              .onFailure(rs -> response.fail(rs)).onSuccess(rs -> {
              RedisAPI.api(getClient()).get(GlobalConstants.RD_LOGIN_COOKIES).onSuccess(
                getRes -> {
                  JsonObject oldCookies = new JsonObject(getRes.toString());
                  JsonObject outCookies = CookiesManager.updateCookies(oldCookies, cookies);
                  List<String> list = Arrays.asList(
                    GlobalConstants.RD_LOGIN_COOKIES,
                    outCookies.toString(),
                    GlobalConstants.TIME_S_MARK,
                    String.valueOf(GlobalConstants.RD_LOGIN_COOKIES_TIMEOUT));
                  RedisAPI.api(getClient()).set(list, ar -> {
                    RedisLockHandler.releaseDistributionLock(GlobalConstants.COOKIES_LOCK, redisOperationId)
                      .onSuccess(re -> {
                      }).onFailure(re -> response.fail(re));
                    response.complete();
                  });
                });
            });
          }
        }).onFailure(res -> {
        response.fail(new RuntimeException("redis error"));
      });
    });
  }

  /**
   * cookies 字符串解析
   *
   * @param input
   * @return
   */
  private static JsonObject cookiesDecode(List<String> input) {
    Iterator iterator = input.iterator();
    JsonObject cookies = new JsonObject();
    while (iterator.hasNext()) {
      JsonObject cookie = new JsonObject();
      String line = iterator.next().toString();
      String[] splited = line.split(";");
      String[] cookieSplit = splited[0].split("=");
      String domain = splited[1].split("=")[1];
      String expires = splited[2].split("=")[1];
      cookie.put("name", cookieSplit[0]);
      cookie.put("value", cookieSplit[1]);
      cookie.put("domain", domain);
      cookie.put("expires", expires);
      cookies.put(cookieSplit[0], cookie);
    }
    return cookies;
  }

  /**
   * cookies合并
   *
   * @param oldCookies
   * @param newCookies
   * @return updatedcookies
   */
  private static JsonObject updateCookies(JsonObject oldCookies, JsonObject newCookies) {
    JsonObject outCookies = oldCookies.copy();
    newCookies.forEach(entry -> {
      log.info(entry.toString());
      String key = entry.getKey();
      outCookies.put(key, newCookies.getJsonObject(key));
    });
    return outCookies;
  }

  /**
   * header bulider
   *
   * @param cookies
   * @return
   */
  private static String cookiesJoin(JsonObject cookies) {
    List cookiesHeader = new ArrayList();
    cookies.forEach(stringObjectEntry -> {
      String key = stringObjectEntry.getKey();
      String value = cookies.getJsonObject(key).getString("value");
      cookiesHeader.add(String.format("%s=%s", key, value));
    });
    return String.join(";", cookiesHeader);
  }

  /**
   * cookies add
   *
   * @param httpRequest
   * @return
   */
  public static Future<JsonObject> headCookiesAdder(HttpRequest<Buffer> httpRequest) {
    return Future.future(result -> {
      RedisAPI.api(getClient()).exists(Collections.singletonList(GlobalConstants.RD_LOGIN_COOKIES))
        .onSuccess(res -> {
          Boolean isExists = res.toBoolean();
          if (!isExists) {
            result.fail(new UnloginException());
          } else {
            String redisOperationId = UUID.randomUUID().toString();
            RedisLockHandler.getDistributionLock(GlobalConstants.COOKIES_LOCK, redisOperationId)
              .onFailure(rs -> result.fail(rs)).onSuccess(rs -> {
              RedisAPI.api(getClient()).get(GlobalConstants.RD_LOGIN_COOKIES).onSuccess(getRes -> {
                JsonObject cookies = new JsonObject(getRes.toString());
                String cookiesHeader = cookiesJoin(cookies);
                httpRequest.headers().set("cookie", cookiesHeader);
                RedisLockHandler.releaseDistributionLock(GlobalConstants.COOKIES_LOCK, redisOperationId)
                  .onSuccess(re -> {
                  }).onFailure(re -> result.fail(re));
                result.complete(cookies);
              }).onFailure(getRes -> {
                result.fail(new RuntimeException("redis Fail"));
              });
            });
          }
        }).onFailure(res -> {
        result.fail(new RuntimeException("redis Fail"));
      });
    });
  }
}
