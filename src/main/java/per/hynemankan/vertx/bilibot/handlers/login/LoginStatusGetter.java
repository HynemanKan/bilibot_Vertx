package per.hynemankan.vertx.bilibot.handlers.login;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.redis.client.RedisAPI;
import lombok.extern.slf4j.Slf4j;
import per.hynemankan.vertx.bilibot.expection.RedisAPIException;
import per.hynemankan.vertx.bilibot.expection.UnhealthyException;
import per.hynemankan.vertx.bilibot.expection.WebClientException;
import per.hynemankan.vertx.bilibot.handlers.common.HealthChecker;
import per.hynemankan.vertx.bilibot.utils.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;

import static per.hynemankan.vertx.bilibot.db.RedisUtils.getClient;
import static per.hynemankan.vertx.bilibot.utils.HeaderAdder.headerAdd;

/**
 * @author hyneman
 */
@Slf4j
public class LoginStatusGetter implements Handler<RoutingContext> {
  private final WebClient webClient;
  private final Vertx vertx;

  public LoginStatusGetter(Vertx vertx, WebClient webClient) {
    this.webClient = webClient;
    this.vertx = vertx;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    if (!HealthChecker.isHealthy()) {
      routingContext.fail(new UnhealthyException());
      return;
    }
    JsonObject data = new JsonObject();
    RedisAPI.api(getClient()).exists(Collections.singletonList(GlobalConstants.RD_LOGIN_OAUTHKEY))
      .onSuccess(res -> {
        URL url;
        Boolean isExists = res.toBoolean();
        if (!isExists) {
          data.put("loginStatus", LoginStatus.OAUTH_TOKEN_NOT_EXIST.name());
          routingContext.response().end(CodeMapping.successResponse(data).toString());
        } else {
          try {
            url = new URL(GlobalConstants.BILI_LOGIN_STATUS_API);
          } catch (MalformedURLException e) {
            routingContext.fail(new WebClientException("Got illegal Url!", e));
            return;
          }
          RedisAPI.api(getClient()).get(GlobalConstants.RD_LOGIN_OAUTHKEY).onSuccess(getres -> {
            String oauthKey = getres.toString();
            log.info(oauthKey);
            HttpRequest<Buffer> request = webClient.post(GlobalConstants.BILI_PORT, url.getHost(), url.getPath());
            headerAdd(request);//请求头user-agent添加
            MultiMap formData = MultiMap.caseInsensitiveMultiMap();
            formData.set("oauthKey", oauthKey);
            request.sendForm(formData).onSuccess(biliResponse ->
              routingContext.response().end(CodeMapping.successResponse(dealResponse(biliResponse)).toString())
            ).onFailure(biliResponse ->
              routingContext.fail(biliResponse)
            );
          }).onFailure(getRes -> {
            throw new RedisAPIException();
          });
        }
      }).onFailure(res -> {
      throw new RedisAPIException();
    });
  }

  private JsonObject dealResponse(HttpResponse<Buffer> httpBody) {
    JsonObject response = httpBody.body().toJsonObject();
    log.info(response.toString());
    JsonObject responseBody = new JsonObject();
    if (response.getBoolean("status")) {
      responseBody.put("loginStatus", LoginStatus.OAUTH_SUCCESS.name());
      log.info(String.format("cookies:%d", httpBody.cookies().size()));
      CookiesManager.updateCookiesFormHttpBody(httpBody).onSuccess(res -> {
        vertx.eventBus().request(EventBusChannels.START_MESSAGE_FETCH.name(), "", r -> {
          if (r.succeeded()) {
            log.info("Message fetch verticle start success!");
          } else {
            log.info(r.cause().toString());
          }
        });
      });
      return responseBody;
    } else {
      switch (response.getInteger("data")) {
        case -2:
          responseBody.put("loginStatus", LoginStatus.OAUTH_TOKEN_TIMEOUT.name());
          break;
        case -4:
          responseBody.put("loginStatus", LoginStatus.OAUTH_TOKEN_UNSCAN.name());
          break;
        case -5:
          responseBody.put("loginStatus", LoginStatus.OAUTH_TOKEN_UNCOMFIRMED.name());
          break;
        default:
          responseBody.put("loginStatus", LoginStatus.OAUTH_TOKEN_ERROR.name());
          break;
      }
      return responseBody;
    }
  }
}
