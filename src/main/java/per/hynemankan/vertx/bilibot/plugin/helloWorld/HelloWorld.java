package per.hynemankan.vertx.bilibot.plugin.helloWorld;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.slf4j.Slf4j;
import per.hynemankan.vertx.bilibot.handlers.message.MessageSenderContorl;
import per.hynemankan.vertx.bilibot.plugin.PluginBaseClass;
import per.hynemankan.vertx.bilibot.utils.GlobalConstants;
import per.hynemankan.vertx.bilibot.utils.PluginStatus;

@Slf4j
public class HelloWorld extends PluginBaseClass {
  public static final String TRIGGER="test";
  public static final String EVENT_BUS_CHANNEL="PLUGIN_HELLO_WORLD";
  public HelloWorld(Vertx vertx, WebClient client, MessageSenderContorl messageSenderContorl) {
    super(vertx, client, messageSenderContorl);
    init(EVENT_BUS_CHANNEL);
  }

  @Override
  public Future<JsonObject> entry(JsonObject messageBody, JsonObject variate, JsonObject shareVariate, Integer selfId, Integer targetId){
    return Future.future(res->{
      JsonObject response = new JsonObject();
      messageSenderContorl.sendTextMessage("demo",selfId,targetId);
      response.put(GlobalConstants.PLUGIN_STATE, PluginStatus.MESSAGE_LOOP_FINISH.name());
      response.put(GlobalConstants.VARIATE,variate);
      response.put(GlobalConstants.SHARE_VARIATE,shareVariate);
      res.complete(response);
    });
  }
}

