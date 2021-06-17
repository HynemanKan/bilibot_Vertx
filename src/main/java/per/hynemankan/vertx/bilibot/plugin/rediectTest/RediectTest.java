package per.hynemankan.vertx.bilibot.plugin.rediectTest;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.slf4j.Slf4j;
import per.hynemankan.vertx.bilibot.plugin.helloWorld.HelloWorld;
import per.hynemankan.vertx.bilibot.utils.GlobalConstants;
import per.hynemankan.vertx.bilibot.utils.PluginStatus;
@Slf4j
public class RediectTest {
  private Vertx vertx;
  private WebClient client;
  public static final String TRIGGER="redirect";
  public static final String EVENT_BUS_CHANNEL="PLUGIN_REDIECT_TEST";

  public String getEventBusChannel() {
    return EVENT_BUS_CHANNEL;
  }

  public String getTrigger() {
    return TRIGGER;
  }

  public RediectTest(Vertx vertx, WebClient client){
    this.vertx=vertx;
    this.client=client;
    init();
  }

  private void init(){
    vertx.eventBus().<JsonObject>consumer(EVENT_BUS_CHANNEL).handler(this::handler);
  }


  private void handler(Message<JsonObject> message){
    JsonObject messageBody = message.body().getJsonObject(GlobalConstants.MESSAGE_BODY);
    JsonObject variate = message.body().getJsonObject(GlobalConstants.VARIATE);
    JsonObject shareVariate = message.body().getJsonObject(GlobalConstants.SHARE_VARIATE);
    log.info("hello world");
    JsonObject response = new JsonObject();
    if(messageBody.containsKey(GlobalConstants.JUMP_BACK)){
      response.put(GlobalConstants.PLUGIN_STATE,PluginStatus.MESSAGE_LOOP_FINISH.name());
    }else{
      response.put(GlobalConstants.PLUGIN_STATE, PluginStatus.MESSAGE_LOOP_REDIRECT.name());
      response.put(GlobalConstants.REDIRECT_TARGET, HelloWorld.EVENT_BUS_CHANNEL);
    }
    response.put(GlobalConstants.VARIATE,variate);
    response.put(GlobalConstants.SHARE_VARIATE,shareVariate);
    message.reply(response);
  }
}
