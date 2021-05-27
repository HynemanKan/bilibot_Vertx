package per.hynemankan.vertx.bilibot;

import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.config.ConfigRetrieverOptions;
import per.hynemankan.vertx.bilibot.utils.EventBusChannels;


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

    // 读取配置(ConfigurationKeys：配置文件的Key,xxx.properties:value)
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
/**
    CompositeFuture.all(mainFuture,)
      .onSuccess(res -> HealthyCheck.checkHealthy(vertx)
        .onSuccess(ar -> {
          log.info("Iflytec offline Adapter deploy success!");
          PeriodJobs.periodCheck(HEALTH_CHECK_PERIOD, vertx);
        })
        .onFailure(ar -> log.error("Healthy check failed,IflytekOffline adapter deploy failed!")))
      .onFailure(res -> log.error("IflytekOffline adapter deploy failed!", res));
*/
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

    ConfigS classpathFile =
      new ConfigStoreOptions()
        .setType("file")
        .setFormat("properties")
        .setConfig(classpathFileConfiguration);

    // 外部配置文件（K8s下使用configMap配置,测试可配置dev或local)
    JsonObject envFileConfiguration = new JsonObject()
      .put("path", "local.properties")
      .put("hierarchical", true);

    ConfigStoreOptions envFile =
      new ConfigStoreOptions()
        .setType("file")
        .setFormat("properties")
        .setConfig(envFileConfiguration)
        .setOptional(true);

    // 默认优先级envFile>classpathFile
    return new ConfigRetrieverOptions()
      .addStore(classpathFile)
      .addStore(envFile)
      .setScanPeriod(5000);
  }
}

