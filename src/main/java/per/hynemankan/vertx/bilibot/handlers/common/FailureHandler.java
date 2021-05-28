package per.hynemankan.vertx.bilibot.handlers.common;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import per.hynemankan.vertx.bilibot.utils.CodeMapping;


@Slf4j
public class FailureHandler implements Handler<RoutingContext> {

  @Override
  public void handle(RoutingContext context) {

    Throwable thrown = context.failure();
    log.error("Request failed,{}", thrown.getCause().getMessage(), thrown);
    JsonObject response = CodeMapping.UNKNOWN_ERROR.toJson();
    response.put("trackBack",thrown.getCause().getMessage());
    context.response().end(response.toString());
  }
}
