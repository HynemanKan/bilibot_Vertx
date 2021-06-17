package per.hynemankan.vertx.bilibot.plugin.helloWorld;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.slf4j.Slf4j;
import per.hynemankan.vertx.bilibot.handlers.message.MessageSender;
import per.hynemankan.vertx.bilibot.utils.GlobalConstants;
import per.hynemankan.vertx.bilibot.utils.PluginStatus;

@Slf4j
public class HelloWorld {
  private Vertx vertx;
  private WebClient client;
  public static final String TRIGGER="test";
  public static final String EVENT_BUS_CHANNEL="PLUGIN_HELLO_WORLD";

  public String getEventBusChannel() {
    return EVENT_BUS_CHANNEL;
  }

  public String getTrigger() {
    return TRIGGER;
  }

  public HelloWorld(Vertx vertx, WebClient client){
    this.vertx=vertx;
    this.client=client;
    init();
  }

  private void init(){
    vertx.eventBus().<JsonObject>consumer(EVENT_BUS_CHANNEL).handler(this::handler);
  }


  private void handler(Message<JsonObject> message){
    JsonObject messageBody = message.body().getJsonObject(GlobalConstants.MESSAGE_BODY);
    Integer selfId = messageBody.getInteger("receiver_id");
    Integer targetId = messageBody.getInteger("sender_uid");
    JsonObject variate = message.body().getJsonObject(GlobalConstants.VARIATE);
    JsonObject shareVariate = message.body().getJsonObject(GlobalConstants.SHARE_VARIATE);
    MessageSender.sendTextMessage(client,"hello world",selfId,targetId)
      .onFailure(err->log.warn(err.getMessage()))
      .onSuccess(res->log.info("Send message success"));
    JsonObject response = new JsonObject();
    response.put(GlobalConstants.PLUGIN_STATE, PluginStatus.MESSAGE_LOOP_FINISH.name());
    response.put(GlobalConstants.VARIATE,variate);
    response.put(GlobalConstants.SHARE_VARIATE,shareVariate);
    message.reply(response);
  }
}
