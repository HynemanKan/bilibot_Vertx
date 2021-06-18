package per.hynemankan.vertx.bilibot.plugin.rediectTest;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.slf4j.Slf4j;
import per.hynemankan.vertx.bilibot.handlers.message.MessageSenderContorl;
import per.hynemankan.vertx.bilibot.plugin.PluginBaseClass;
import per.hynemankan.vertx.bilibot.plugin.helloWorld.HelloWorld;
import per.hynemankan.vertx.bilibot.utils.GlobalConstants;
import per.hynemankan.vertx.bilibot.utils.PluginStatus;

/**
 * @author hyneman
 */
@Slf4j
public class RedirectTest extends PluginBaseClass {
  public static String TRIGGER="redirect";
  public static String EVENT_BUS_CHANNEL="PLUGIN_REDIRECT_TEST";
  public RedirectTest(Vertx vertx, WebClient client, MessageSenderContorl messageSenderContorl) {
    super(vertx, client, messageSenderContorl);
    init(EVENT_BUS_CHANNEL);
  }
  @Override
  public JsonObject entry(JsonObject messageBody,JsonObject variate,JsonObject shareVariate,Integer selfId,Integer targetId){
    JsonObject response = new JsonObject();
    if(messageBody.containsKey(GlobalConstants.JUMP_BACK)){
      messageSenderContorl.sendTextMessage("Jump back",selfId,targetId);
      response.put(GlobalConstants.PLUGIN_STATE,PluginStatus.MESSAGE_LOOP_FINISH.name());
    }else{
      messageSenderContorl.sendTextMessage("call redirect",selfId,targetId);
      response.put(GlobalConstants.PLUGIN_STATE, PluginStatus.MESSAGE_LOOP_REDIRECT.name());
      response.put(GlobalConstants.REDIRECT_TARGET, HelloWorld.EVENT_BUS_CHANNEL);
    }
    response.put(GlobalConstants.VARIATE,variate);
    response.put(GlobalConstants.SHARE_VARIATE,shareVariate);
    return response;
  }
}
