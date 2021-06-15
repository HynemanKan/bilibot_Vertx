package per.hynemankan.vertx.bilibot.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import lombok.extern.slf4j.Slf4j;
import per.hynemankan.vertx.bilibot.expection.MessageDealException;
import per.hynemankan.vertx.bilibot.expection.StopPeriodicException;
import per.hynemankan.vertx.bilibot.expection.TryDoubleStartPeriodicException;
import per.hynemankan.vertx.bilibot.handlers.message.MessageGetter;
import per.hynemankan.vertx.bilibot.utils.EventBusChannels;
import per.hynemankan.vertx.bilibot.utils.GlobalConstants;
@Slf4j
public class MessageFetchVerticle extends AbstractVerticle {
  private boolean messageFetchStatus=false;
  private Long timerId;
  private WebClient client;
  private Long lastFetchTimestamp;

  @Override
  public void start(Promise<Void> startPromise) {
    WebClientOptions options = new WebClientOptions()
      .setMaxPoolSize(100)
//                .setConnectTimeout(3000)
      .setIdleTimeout(10)
      .setMaxWaitQueueSize(50);
    client = WebClient.create(vertx, options);
    vertx.eventBus().<String>consumer(EventBusChannels.START_MESSAGE_FETCH.name()).handler(this::startMessageFetch);
    vertx.eventBus().<String>consumer(EventBusChannels.END_MESSAGE_FETCH.name()).handler(this::endMessageFetch);
    log.info("Init message fetch success!");
    lastFetchTimestamp = System.currentTimeMillis()*1000;
    startPromise.complete();
  }

  private void startMessageFetch(Message<String> message){
    if(!messageFetchStatus){

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
   * "last_msg":
   * @param session
   */
  private void dealMessageSession(Object session){
    if(session instanceof JsonObject){
      Integer talkerId = ((JsonObject) session).getInteger("talker_id");
      Integer unread = ((JsonObject) session).getInteger("unread_count");
      log.info(String.format("deal message from %d", talkerId));
      if (unread>1){
        log.info("unread >1");
      }else{
        JsonObject message = ((JsonObject) session).getJsonObject("last_msg");
        log.info(message.toString());
      }
    }else{
      log.warn(session.toString());
      throw new MessageDealException("unexcept type");
    }
  }

  private void checkUnreadMessage(Long id){
    log.info("check unread message");
    Long newFetchTimeStamp = System.currentTimeMillis()*1000;
    MessageGetter.getMessageSession(client,lastFetchTimestamp)
      .onFailure(err->{
        log.warn(err.getMessage());
        this.timerId = vertx.setTimer(GlobalConstants.MESSAGE_FETCH_PERIOD,this::checkUnreadMessage);
      }).onSuccess(data->{
        this.lastFetchTimestamp = newFetchTimeStamp;
        Integer count = data.getInteger("count");
        log.info(String.format("fetch new Session:%d", count));
        if(count>0){
          data.getJsonArray("session_list").forEach(this::dealMessageSession);
        }
        this.timerId = vertx.setTimer(GlobalConstants.MESSAGE_FETCH_PERIOD,this::checkUnreadMessage);
    });

  }


}
