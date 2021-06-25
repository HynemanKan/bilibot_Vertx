package per.hynemankan.vertx.bilibot.plugin;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import org.jetbrains.annotations.NotNull;
import per.hynemankan.vertx.bilibot.handlers.message.MessageSenderContorl;
import per.hynemankan.vertx.bilibot.plugin.helloWorld.HelloWorld;
import per.hynemankan.vertx.bilibot.utils.GlobalConstants;
import per.hynemankan.vertx.bilibot.utils.PluginStatus;

public class PluginBaseClass {
  public Vertx vertx;
  public WebClient client;
  public static String TRIGGER;
  public static String EVENT_BUS_CHANNEL;
  public MessageSenderContorl messageSenderContorl;

  public String getEventBusChannel() {
    return EVENT_BUS_CHANNEL;
  }

  public String getTrigger() {
    return TRIGGER;
  }

  public PluginBaseClass(Vertx vertx, WebClient client, MessageSenderContorl messageSenderContorl) {
    this.vertx = vertx;
    this.client = client;
    this.messageSenderContorl = messageSenderContorl;
  }

  public void init(String eventBusChannel) {
    vertx.eventBus().<JsonObject>consumer(eventBusChannel).handler(this::handler);
  }

  public Future<JsonObject> entry(JsonObject messageBody, JsonObject variate, JsonObject shareVariate, Integer selfId, Integer targetId) {
    return Future.future(res -> {
      JsonObject response = new JsonObject();
      messageSenderContorl.sendTextMessage("demo", targetId);
      response.put(GlobalConstants.PLUGIN_STATE, PluginStatus.MESSAGE_LOOP_FINISH.name());
      response.put(GlobalConstants.VARIATE, variate);
      response.put(GlobalConstants.SHARE_VARIATE, shareVariate);
      res.complete(response);
    });
  }


  private void handler(@NotNull Message<JsonObject> message) {
    JsonObject messageBody = message.body().getJsonObject(GlobalConstants.MESSAGE_BODY);
    Integer selfId = messageBody.getInteger("receiver_id");
    Integer targetId = messageBody.getInteger("sender_uid");
    JsonObject variate = message.body().getJsonObject(GlobalConstants.VARIATE);
    JsonObject shareVariate = message.body().getJsonObject(GlobalConstants.SHARE_VARIATE);
    entry(messageBody, variate, shareVariate, selfId, targetId)
      .onSuccess(message::reply).onFailure(err -> {
      message.fail(-1, "deal error");
    });
  }
}
