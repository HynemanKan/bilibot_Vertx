package per.hynemankan.vertx.bilibot.handlers.info;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import per.hynemankan.vertx.bilibot.expection.UnhealthyException;
import per.hynemankan.vertx.bilibot.handlers.common.HealthChecker;
import per.hynemankan.vertx.bilibot.utils.CodeMapping;

public class BaseInfoGetter implements Handler<RoutingContext> {
  private final WebClient webClient;

  public BaseInfoGetter(WebClient webClient){
    this.webClient = webClient;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    if (!HealthChecker.isHealthy()) {
      routingContext.fail(new UnhealthyException());
      return;
    }
    GetBaseInfo.getBaseInfo(webClient).onSuccess(res->{
      routingContext.response().end(CodeMapping.successResponse(res).toString());
    }).onFailure(res->{
      routingContext.fail(res);
    });
  }
}
