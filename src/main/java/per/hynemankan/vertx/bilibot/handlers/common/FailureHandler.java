package per.hynemankan.vertx.bilibot.handlers.common;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import per.hynemankan.vertx.bilibot.utils.CodeMapping;


@Slf4j
public class FailureHandler implements Handler<RoutingContext> {

  @Override
  public void handle(RoutingContext context) {

    Throwable thrown = context.failure();
    log.warn(String.format("Request failed,%s",thrown.toString()));
    JsonObject response = CodeMapping.UNKNOWN_ERROR.toJson();
    response.put("trackBack",thrown.toString());
    context.response().end(response.toString());
  }
}
