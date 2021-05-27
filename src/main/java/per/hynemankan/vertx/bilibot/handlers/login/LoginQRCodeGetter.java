package per.hynemankan.vertx.bilibot.handlers.login;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.Response;
import io.vertx.redis.client.impl.RedisClient;
import lombok.extern.slf4j.Slf4j;
import  per.hynemankan.vertx.bilibot.utils.GlobalConstants;

import java.util.Collections;
import static per.hynemankan.vertx.bilibot.db.RedisUtils.getClient;


@Slf4j
public class LoginQRCodeGetter implements Handler<RoutingContext>{
  private final WebClient webClient;



  public LoginQRCodeGetter(WebClient webClient, RedisClient redisClient){
    this.webClient = webClient;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    RedisAPI.api(getClient()).exists(Collections.singletonList(GlobalConstants.RD_LOGIN_COOKIES))
      .onSuccess(res->{
        routingContext.response().end();
      }).onFailure(res->{
        //TODO
    });
  }
}
