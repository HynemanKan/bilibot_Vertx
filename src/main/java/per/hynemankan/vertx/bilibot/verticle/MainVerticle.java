package per.hynemankan.vertx.bilibot.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import lombok.extern.slf4j.Slf4j;
import per.hynemankan.vertx.bilibot.utils.GlobalConstants;

@Slf4j
public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise){
    vertx.createHttpServer().requestHandler(req -> {
      req.response()
        .putHeader("content-type", "text/plain")
        .end("Hello from Vert.x!");
    }).listen(GlobalConstants.HTTP_PORT, http -> {
      if (http.succeeded()) {
        startPromise.complete();
        log.info("http server on");
      } else {
        startPromise.fail(http.cause());
      }
    });
  }
}
