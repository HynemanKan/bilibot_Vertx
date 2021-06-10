package per.hynemankan.vertx.bilibot.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.handler.BodyHandler;
import lombok.extern.slf4j.Slf4j;
import per.hynemankan.vertx.bilibot.handlers.common.FailureHandler;
import per.hynemankan.vertx.bilibot.handlers.common.PingHandler;
import per.hynemankan.vertx.bilibot.handlers.info.BaseInfoGetter;
import per.hynemankan.vertx.bilibot.handlers.login.LoginQRCodeGetter;
import per.hynemankan.vertx.bilibot.handlers.login.LoginStatusGetter;
import per.hynemankan.vertx.bilibot.utils.GlobalConstants;

@Slf4j
public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise){
    WebClientOptions options = new WebClientOptions()
      .setMaxPoolSize(100)
//                .setConnectTimeout(3000)
      .setIdleTimeout(10)
      .setMaxWaitQueueSize(50);
    WebClient client = WebClient.create(vertx, options);

    PingHandler pingHandler = new PingHandler();
    FailureHandler failureHandler = new FailureHandler();

    LoginQRCodeGetter loginQRCodeGetter = new LoginQRCodeGetter(client);
    LoginStatusGetter loginStatusGetter = new LoginStatusGetter(client);

    BaseInfoGetter baseInfoGetter = new BaseInfoGetter(client);

    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    router.route().consumes("application/json");
    router.route().produces("application/json");
    router.get("/ping").handler(pingHandler).failureHandler(failureHandler);
    //login
    router.get("/API/login/getQRCode").handler(loginQRCodeGetter).failureHandler(failureHandler);
    router.get("/API/login/getLoginStatus").handler(loginStatusGetter).failureHandler(failureHandler);
    //info
    router.get("/API/info/baseInfo").handler(baseInfoGetter).failureHandler(failureHandler);
    // 启动Http server
    vertx.createHttpServer().requestHandler(router).listen(GlobalConstants.HTTP_PORT, r -> {
      if (r.succeeded()) {
        log.info("Http server created!");
        startPromise.complete();
      } else {
        log.error("Http server create failed!", r.cause());
        startPromise.fail(r.cause());
      }
    });

  }
}
