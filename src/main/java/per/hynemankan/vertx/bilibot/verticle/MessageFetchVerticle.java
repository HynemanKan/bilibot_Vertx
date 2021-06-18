package per.hynemankan.vertx.bilibot.verticle;

import java.util.*;
import java.util.regex.Pattern;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.redis.client.RedisAPI;
import lombok.extern.slf4j.Slf4j;
import per.hynemankan.vertx.bilibot.expection.MessageDealException;
import per.hynemankan.vertx.bilibot.expection.RedisAPIException;
import per.hynemankan.vertx.bilibot.expection.StopPeriodicException;
import per.hynemankan.vertx.bilibot.expection.TryDoubleStartPeriodicException;
import per.hynemankan.vertx.bilibot.handlers.message.MessageGetter;
import per.hynemankan.vertx.bilibot.handlers.message.MessageSenderContorl;
import per.hynemankan.vertx.bilibot.handlers.message.UpdateAlreadyReadHandler;
import per.hynemankan.vertx.bilibot.plugin.helloWorld.HelloWorld;
import per.hynemankan.vertx.bilibot.plugin.rediectTest.RedirectTest;
import per.hynemankan.vertx.bilibot.utils.EventBusChannels;
import per.hynemankan.vertx.bilibot.utils.GlobalConstants;
import per.hynemankan.vertx.bilibot.utils.PluginStatus;
import static per.hynemankan.vertx.bilibot.db.RedisUtils.getClient;

/**
 * 消息抓取和处理 verticle
 * @author hyneman
 */
@Slf4j
public class MessageFetchVerticle extends AbstractVerticle {
  private boolean messageFetchStatus=false;
  private Long timerId;
  private WebClient client;
  private Long lastFetchTimestamp;
  private HashMap<String,String> pluginMap=new HashMap<>();
  private MessageSenderContorl messageSenderContorl;
  @Override
  public void start(Promise<Void> startPromise) {
    WebClientOptions options = new WebClientOptions()
      .setMaxPoolSize(100)
      .setConnectTimeout(3000)
      .setIdleTimeout(10)
      .setMaxWaitQueueSize(50);
    client = WebClient.create(vertx, options);
    vertx.eventBus().<String>consumer(EventBusChannels.START_MESSAGE_FETCH.name()).handler(this::startMessageFetch);
    vertx.eventBus().<String>consumer(EventBusChannels.END_MESSAGE_FETCH.name()).handler(this::endMessageFetch);
    log.info("Init message fetch success!");
    lastFetchTimestamp = System.currentTimeMillis()*1000;
    this.messageSenderContorl = new MessageSenderContorl(vertx,client);
    pluginRegister();
    startPromise.complete();
  }

  /**
   * 插件加载，待实现动态加载卸载插件
   */
  private void pluginRegister(){
    HelloWorld helloWorld = new HelloWorld(vertx,client,messageSenderContorl);
    RedirectTest redirectTest = new RedirectTest(vertx,client,messageSenderContorl);
    pluginMap.put(HelloWorld.TRIGGER,HelloWorld.EVENT_BUS_CHANNEL);
    pluginMap.put(RedirectTest.TRIGGER, RedirectTest.EVENT_BUS_CHANNEL);
  }


  private void startMessageFetch(Message<String> message){
    if(!messageFetchStatus){
      messageSenderContorl.startTimer();
      this.timerId = vertx.setTimer(GlobalConstants.MESSAGE_FETCH_PERIOD,this::checkUnreadMessage);
      message.reply(true);
    }else{
      throw new TryDoubleStartPeriodicException();
    }
  }

  private void endMessageFetch(Message<String> message){
    if(messageFetchStatus){
      vertx.cancelTimer(this.timerId);
      message.reply(true);
    }else{
      throw new StopPeriodicException();
    }
  }

  /**
   * session 会话处理
   * "talker_id": 632175529,
   * "session_type": 1,
   * "top_ts": 0,
   * "is_follow": 0,
   * "is_dnd": 0,
   * "ack_seqno": 225,
   * "ack_ts": 1600750734934625,
   * "session_ts": 1623586585967790,
   * "unread_count": 5,
   * "last_msg":{}
   * @param session
   */
  private void dealMessageSession(Object session){
    if(session instanceof JsonObject){
      Integer talkerId = ((JsonObject) session).getInteger("talker_id");
      log.info(String.format("deal message from %d", talkerId));
      JsonObject message = ((JsonObject) session).getJsonObject("last_msg");
      dealMessage(message);
    }else{
      log.warn(session.toString());
      throw new MessageDealException("unexcept type");
    }
  }
  /**
   * 相应入口,传入message
   * "sender_uid": 12076317,
   * "receiver_type": 1,
   * "receiver_id": 7838945,
   * "msg_type": 10,
   * "content": json message body,
   * "msg_seqno": 56,
   * "timestamp": 1623804526,
   * "msg_key": 6974187335964454634,
   * "msg_status": 0,
   * "notify_code": "3_13",
   * "new_face_version": 1
   * @param message
   */
  private void doMessageRoute(JsonObject message){
    Integer talkerId = message.getInteger("sender_uid");
    String redisKey = String.format(GlobalConstants.RD_SESSION_KEY,talkerId);
    RedisAPI.api(getClient()).exists(Collections.singletonList(redisKey))
      .onFailure(err->{
        log.warn(err.getMessage());
        throw new RedisAPIException();
      }).onSuccess(redisRes->{
        Boolean isExists = redisRes.toBoolean();
        if(!isExists){
          JsonArray routeStack = new JsonArray();
          JsonObject variates = new JsonObject();
          messageRoute(routeStack,variates,message,redisKey);
        }else{
          RedisAPI.api(getClient()).get(redisKey)
            .onFailure(err->{
              log.warn(err.getMessage());
              throw new RedisAPIException();
            }).onSuccess(getRes->{
              JsonObject collection = new JsonObject(getRes.toString());
              JsonArray routeStack = collection.getJsonArray(GlobalConstants.ROUTE_STACK);
              JsonObject variates = collection.getJsonObject(GlobalConstants.VARTATES);
              messageRoute(routeStack,variates,message,redisKey);
          });
        }
    });
  }

  /**
   * 前相应消息路由
   * @param routeStack
   * @param variates
   * @param message
   * @param redisKey
   */
  private void messageRoute(JsonArray routeStack,JsonObject variates,JsonObject message,String redisKey){
    JsonObject packagedData = new JsonObject();
    if (routeStack.size()==0){
      packagedData.put(GlobalConstants.MESSAGE_BODY,message)
        .put(GlobalConstants.VARIATE,new JsonObject())
        .put(GlobalConstants.SHARE_VARIATE,new JsonObject());
      String matchPluginAddress = doPluginMatch(message);
      if("NaN".equals(matchPluginAddress)){
        log.info("No plugin activate");
        return;
      }
      routeStack.add(matchPluginAddress);
      vertx.eventBus().request(matchPluginAddress,packagedData)
        .onFailure(err->log.warn(err.getMessage()))
        .onSuccess(response->{
          JsonObject pluginResponse = (JsonObject) response.body();
          afterPluginActiveRoute(routeStack,variates,pluginResponse,message,matchPluginAddress,redisKey);
        });
    }else{
      String pluginAddress = routeStack.getString(routeStack.size()-1);
      packagedData.put(GlobalConstants.MESSAGE_BODY,message)
        .put(GlobalConstants.VARIATE,variates.getJsonObject(pluginAddress))
        .put(GlobalConstants.SHARE_VARIATE,variates.getJsonObject(GlobalConstants.SHARE_VARIATE));
      vertx.eventBus().request(pluginAddress,packagedData)
        .onFailure(err->log.warn(err.getMessage()))
        .onSuccess(response->{
          JsonObject pluginResponse = (JsonObject) response.body();
          afterPluginActiveRoute(routeStack,variates,pluginResponse,message,pluginAddress,redisKey);
        });
    }
  }

  /**
   * 响应后二次路由
   * @param routeStack
   * @param variates
   * @param pluginResponse
   * @param matchPluginAddress
   * @param redisKey
   */
  private void afterPluginActiveRoute(JsonArray routeStack,JsonObject variates,JsonObject pluginResponse,JsonObject message,String matchPluginAddress,String redisKey){
    variates.put(matchPluginAddress,pluginResponse.getJsonObject(GlobalConstants.VARIATE));
    variates.put(GlobalConstants.SHARE_VARIATE,pluginResponse.getJsonObject(GlobalConstants.SHARE_VARIATE));
    switch(PluginStatus.valueOf(pluginResponse.getString(GlobalConstants.PLUGIN_STATE))){
      case MESSAGE_LOOP_FINISH:
        if(routeStack.size()>1){
          String finishedAddress = messageStackPop(routeStack);
          JsonObject JumpBackBody = message.copy()
            .put(GlobalConstants.JUMP_BACK,finishedAddress);
          log.info(String.format("Session %s jump back %s", redisKey,routeStack.getString(routeStack.size()-1)));
          messageRoute(routeStack,variates,JumpBackBody,redisKey);
        }else{
          log.info(String.format("Session %s finish", redisKey));
          delSession(redisKey);
        }
        break;
      case MESSAGE_LOOP_WAIT:
        JsonObject packagedData = new JsonObject()
          .put(GlobalConstants.ROUTE_STACK,routeStack)
          .put(GlobalConstants.VARTATES,variates);
        saveSession(redisKey,packagedData);
        break;
      case MESSAGE_LOOP_REDIRECT:
        String redirectAddress = pluginResponse.getString(GlobalConstants.REDIRECT_TARGET);
        routeStack.add(redirectAddress);
        JsonObject redirectBody = message.copy()
          .put(GlobalConstants.REDIRECT_FROM,routeStack.getString(routeStack.size()-2));
        log.info(String.format("Session %s redirect to %s", redisKey,redirectAddress));
        messageRoute(routeStack,variates,redirectBody,redisKey);
        break;
      case MESSAGE_LOOP_KILL:
        log.warn(String.format("Session %s kill by Plugin %s",redisKey,messageStackPop(routeStack)));
        delSession(redisKey);
        break;
      default:
        log.warn(String.format("Session %s Unknown plugin status", redisKey));
        delSession(redisKey);
    }
  }

  private String messageStackPop(JsonArray routeStack){
    if(routeStack.size()==0){
      throw new ArrayIndexOutOfBoundsException();
    }
    String need = routeStack.getString(routeStack.size()-1);
    routeStack.remove(routeStack.size()-1);
    return need;
  }

  private void saveSession(String redisKey,JsonObject packagedData){
    List<String> list = Arrays.asList(
      GlobalConstants.RD_LOGIN_OAUTHKEY,
      packagedData.toString(),
      GlobalConstants.TIME_S_MARK,
      String.valueOf(GlobalConstants.RD_SESSION_TIMEOUT));
    RedisAPI.api(getClient()).set(list)
      .onFailure(err->{log.warn(err.getMessage());})
      .onSuccess(res->log.info(String.format("%s message active save", redisKey)));
  }

  private void delSession(String redisKey){
    RedisAPI.api(getClient()).del(Collections.singletonList(redisKey))
      .onFailure(err->{log.warn(err.getMessage());})
      .onSuccess(res->log.info(String.format("%s message active finish", redisKey)));
  }

  /**
   * 消息体正则匹配
   * @param message
   * @return
   */
  private String doPluginMatch(JsonObject message){
    Iterator iterator = pluginMap.entrySet().iterator();
    String content = messageFromat(message);
    log.info(content);
    while (iterator.hasNext()){
      Map.Entry entry = (Map.Entry) iterator.next();
      Boolean isMatch = Pattern.matches((String) entry.getKey(),content);
      if(isMatch){
        return (String) entry.getValue();
      }
    }
    return "NaN";
  }

  /**
   * 消息体统一string化
   * @param message
   * @return
   */
  private String messageFromat(JsonObject message){
    JsonObject messageBody = new JsonObject(message.getString("content"));
    switch (message.getInteger("msg_type")){
      case 1:
        return messageBody.getString("content");
      case 2:
        return String.format("[img][%s]",messageBody.getString("url"));
      default:
        return "unknown";
    }
  }

  /**
   * "sender_uid": 12076317,
   * "receiver_type": 1,
   * "receiver_id": 7838945,
   * "msg_type": 10,
   * "content": json message body,
   * "msg_seqno": 56,
   * "timestamp": 1623804526,
   * "msg_key": 6974187335964454634,
   * "msg_status": 0,
   * "notify_code": "3_13",
   * "new_face_version": 1
   * @param message
   */
  private void dealMessage(JsonObject message){
    log.info(message.getString("content"));
    UpdateAlreadyReadHandler.doUpdate(client,message.getInteger("sender_uid"),message.getInteger("msg_seqno"))
      .onFailure(err->{
        log.warn(err.getMessage(),err);
        doMessageRoute(message);
      })
      .onSuccess(res->doMessageRoute(message));
  }



  /**
   *定时抓取消息
   * @param id timerid useless
   */
  private void checkUnreadMessage(Long id){
    log.info("check unread message");
    Long newFetchTimeStamp = System.currentTimeMillis()*1000;
    MessageGetter.getMessageSession(client,lastFetchTimestamp)
      .onFailure(err->{
        log.warn(err.getMessage());
        this.timerId = vertx.setTimer(GlobalConstants.MESSAGE_FETCH_PERIOD,this::checkUnreadMessage);
      }).onSuccess(data->{
        Integer count = data.getInteger("count");
        log.info(String.format("fetch new Session:%d at %d", count,lastFetchTimestamp));
        if(count>0){
          this.lastFetchTimestamp = data.getJsonArray("session_list").getJsonObject(0).getLong("session_ts");
          data.getJsonArray("session_list").forEach(this::dealMessageSession);
        }
        this.timerId = vertx.setTimer(GlobalConstants.MESSAGE_FETCH_PERIOD,this::checkUnreadMessage);
    });

  }


}
