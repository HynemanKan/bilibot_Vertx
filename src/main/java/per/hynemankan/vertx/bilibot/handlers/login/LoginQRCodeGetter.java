package per.hynemankan.vertx.bilibot.handlers.login;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.node.BooleanNode;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.redis.client.RedisAPI;
import lombok.extern.slf4j.Slf4j;
import per.hynemankan.vertx.bilibot.expection.UnhealthyException;
import per.hynemankan.vertx.bilibot.expection.WebClientException;
import per.hynemankan.vertx.bilibot.handlers.common.HealthChecker;
import per.hynemankan.vertx.bilibot.utils.GlobalConstants;
import per.hynemankan.vertx.bilibot.utils.CodeMapping;
import static per.hynemankan.vertx.bilibot.db.RedisUtils.getClient;
import static per.hynemankan.vertx.bilibot.utils.HeaderAdder.headerAdd;

@Slf4j
public class LoginQRCodeGetter implements Handler<RoutingContext>{
  private final WebClient webClient;



  public LoginQRCodeGetter(WebClient webClient){
    this.webClient = webClient;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    if (!HealthChecker.isHealthy()) {
      routingContext.fail(new UnhealthyException());
      return;
    }
    RedisAPI.api(getClient()).exists(Collections.singletonList(GlobalConstants.RD_LOGIN_COOKIES))
      .onSuccess(res->{
        URL url;
        Boolean isExists = res.toBoolean();
        if(isExists){
          routingContext.response().end(CodeMapping.TRY_DOUBLE_LOGIN.toJson().toString());
        }else{
          try {
            url = new URL(GlobalConstants.BILI_LOGIN_QRCODE_GET_API);
          } catch (MalformedURLException e) {
            routingContext.fail(new WebClientException("Got illegal Url!", e));
            return;
          }
          HttpRequest<Buffer> request = webClient.get(80, url.getHost(), url.getPath())
            .expect(ResponsePredicate.status(200));
          headerAdd(request);//请求头user-agent添加
          request.send().onSuccess(response->{
            JsonObject body = response.body().toJsonObject();
            JsonObject resData = new JsonObject();
            String QRCodeUrl = body.getJsonObject("data").getString("url");
            String oauthKey = body.getJsonObject("data").getString("oauthKey");
            List<String> list = Arrays.asList(
              GlobalConstants.RD_LOGIN_OAUTHKEY,
              oauthKey,
              GlobalConstants.TIME_S_MARK,
              String.valueOf(GlobalConstants.RD_LOGIN_OAUTHKEY_TIMEOUT));
            RedisAPI.api(getClient()).set(list,ar->{});
            resData.put("url",QRCodeUrl);
            JsonObject responseJson = CodeMapping.SUCCESS.toJson();
            responseJson.put("data",resData);
            routingContext.response().end(responseJson.toString());
          }).onFailure(response->{
            routingContext.fail(new WebClientException(response.getMessage()));
          });
        }
      }).onFailure(res->{
        throw new RuntimeException("redis Fail");
    });
  }
}
