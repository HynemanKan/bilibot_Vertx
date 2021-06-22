package per.hynemankan.vertx.bilibot;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.config.ConfigRetrieverOptions;
import lombok.extern.slf4j.Slf4j;

import per.hynemankan.vertx.bilibot.db.MysqlUtils;
import per.hynemankan.vertx.bilibot.handlers.common.ExceptionHandler;
import per.hynemankan.vertx.bilibot.handlers.common.HealthChecker;
import per.hynemankan.vertx.bilibot.handlers.common.RedisLockHandler;
import per.hynemankan.vertx.bilibot.utils.EventBusChannels;
import per.hynemankan.vertx.bilibot.verticle.MainVerticle;
import per.hynemankan.vertx.bilibot.db.RedisUtils;
import per.hynemankan.vertx.bilibot.verticle.MessageFetchVerticle;

@Slf4j
public class Application {
  private static Vertx vertx;

  public static void main(String[] args) {
    // 日志接口设置
    System.setProperty("vertx.log-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");

    // vert部署参数设置
    VertxOptions vertxOptions = new VertxOptions()
      // 阻塞警告时间（调试期间设置大值，默认5秒）
      .setWarningExceptionTime(2000000000)
//                 阻塞线程检查的时间间隔(调试期间设置大值，默认1秒）
      .setBlockedThreadCheckInterval(2000000000)
      // 工作线程池大小，默认20
      .setWorkerPoolSize(50);
    vertx = Vertx.vertx(vertxOptions);

    // 读取配置
    ConfigRetrieverOptions configRetrieverOptions = getConfigRetrieverOptions();
    ConfigRetriever configRetriever = ConfigRetriever.create(vertx, configRetrieverOptions);
    configRetriever.getConfig(
      ar -> {
        // cpu核数
        int instances = Runtime.getRuntime().availableProcessors();
        // 根据读取的配置文件和核数部署vertx
        DeploymentOptions deploymentOptions =
          new DeploymentOptions().setInstances(instances).setConfig(ar.result());
        vertx.exceptionHandler(new ExceptionHandler());
        deploy(deploymentOptions);
        RedisLockHandler.init(vertx);
      });

    // 监听配置文件更改（5秒）
    configRetriever.listen(
      change ->

      {
        JsonObject updatedConfiguration = change.getNewConfiguration();
        vertx.eventBus().publish(EventBusChannels.CONFIGURATION_CHANGED.name(), updatedConfiguration);
      });
  }

  private static void deploy(DeploymentOptions deploymentOptions) {
    Future<Void> mainFuture = deployVertical(MainVerticle.class, deploymentOptions);
    Future<Void> redisFuture = deployVertical(RedisUtils.class, deploymentOptions);
    Future<Void> mysqlFuture = deployVertical(MysqlUtils.class, deploymentOptions);
    Future<Void> messageFuture = deployMessageFetch();
    CompositeFuture.all(mainFuture, redisFuture, mysqlFuture, messageFuture)
      .onSuccess(res -> {
        HealthChecker.checkHealthy(vertx).onSuccess(ar -> {
          log.info("health check pass, deploy success!");
        }).onFailure(ar -> {
          log.info("health check fail, deploy fail!");
        });
      }).onFailure(res -> {
      log.error("deploy failed!{}", res.getMessage(), res);
    });

  }

  private static Future<Void> deployMessageFetch() {
    return Future.future(result -> {
      DeploymentOptions deploymentOptions = new DeploymentOptions().setInstances(1);
      vertx.deployVerticle(MessageFetchVerticle.class, deploymentOptions, r -> {
        if (r.succeeded()) {
          result.complete();
        } else {
          result.fail(r.cause());
        }
      });
    });
  }

  private static Future<Void> deployVertical(Class<? extends Verticle> verticleClass, DeploymentOptions option) {
    return Future.future(result -> vertx.deployVerticle(verticleClass, option, r -> {
      if (r.succeeded()) {
        result.complete();
      } else {
        result.fail(r.cause());
      }
    }));
  }

  private static ConfigRetrieverOptions getConfigRetrieverOptions() {
    // 默认配置文件
    JsonObject classpathFileConfiguration = new JsonObject()
      .put("path", "local.properties")
      .put("hierarchical", true);

    ConfigStoreOptions classpathFile =
      new ConfigStoreOptions()
        .setType("file")
        .setFormat("properties")
        .setConfig(classpathFileConfiguration);

    return new ConfigRetrieverOptions()
      .addStore(classpathFile)
      .setScanPeriod(5000);
  }
}

