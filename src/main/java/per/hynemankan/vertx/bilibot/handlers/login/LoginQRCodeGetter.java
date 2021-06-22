package per.hynemankan.vertx.bilibot.handlers.login;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.redis.client.RedisAPI;
import lombok.extern.slf4j.Slf4j;
import per.hynemankan.vertx.bilibot.expection.RedisAPIException;
import per.hynemankan.vertx.bilibot.expection.UnhealthyException;
import per.hynemankan.vertx.bilibot.expection.WebClientException;
import per.hynemankan.vertx.bilibot.handlers.common.HealthChecker;
import per.hynemankan.vertx.bilibot.utils.EventBusChannels;
import per.hynemankan.vertx.bilibot.utils.GlobalConstants;
import per.hynemankan.vertx.bilibot.utils.CodeMapping;

import static per.hynemankan.vertx.bilibot.db.RedisUtils.getClient;
import static per.hynemankan.vertx.bilibot.utils.HeaderAdder.headerAdd;

@Slf4j
public class LoginQRCodeGetter implements Handler<RoutingContext> {
  private final WebClient webClient;
  private final Vertx vertx;


  public LoginQRCodeGetter(Vertx vertx, WebClient webClient) {
    this.vertx = vertx;
    this.webClient = webClient;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    if (!HealthChecker.isHealthy()) {
      routingContext.fail(new UnhealthyException());
      return;
    }
    RedisAPI.api(getClient()).exists(Collections.singletonList(GlobalConstants.RD_LOGIN_COOKIES))
      .onSuccess(res -> {
        URL url;
        Boolean isExists = res.toBoolean();
        if (isExists) {
          routingContext.response().end(CodeMapping.TRY_DOUBLE_LOGIN.toJson().toString());
          vertx.eventBus().request(EventBusChannels.START_MESSAGE_FETCH.name(), "", r -> {
            if (r.succeeded()) {
              log.info("Message fetch verticle start success!");
            } else {
              log.info(r.cause().toString());
            }
          });
        } else {
          try {
            url = new URL(GlobalConstants.BILI_LOGIN_QRCODE_GET_API);
          } catch (MalformedURLException e) {
            routingContext.fail(new WebClientException("Got illegal Url!", e));
            return;
          }
          HttpRequest<Buffer> request = webClient.get(GlobalConstants.BILI_PORT, url.getHost(), url.getPath());
          headerAdd(request);//请求头user-agent添加
          request.send().onSuccess(response -> {
            JsonObject body = response.body().toJsonObject();
            JsonObject resData = new JsonObject();
            String QRCodeUrl = body.getJsonObject("data").getString("url");
            String oauthKey = body.getJsonObject("data").getString("oauthKey");
            List<String> list = Arrays.asList(
              GlobalConstants.RD_LOGIN_OAUTHKEY,
              oauthKey,
              GlobalConstants.TIME_S_MARK,
              String.valueOf(GlobalConstants.RD_LOGIN_OAUTHKEY_TIMEOUT));
            RedisAPI.api(getClient()).set(list, ar -> {
            });
            resData.put("url", QRCodeUrl);
            routingContext.response().end(CodeMapping.successResponse(resData).toString());
          }).onFailure(response -> {
            routingContext.fail(new WebClientException(response.getMessage()));
          });
        }
      }).onFailure(res -> {
      throw new RedisAPIException();
    });
  }
}
