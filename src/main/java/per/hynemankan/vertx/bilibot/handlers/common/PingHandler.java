package per.hynemankan.vertx.bilibot.handlers.common;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class PingHandler implements Handler<RoutingContext> {

  private String message = "pong";

  @Override
  public void handle(RoutingContext context) {
    context.response().end(message);
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
