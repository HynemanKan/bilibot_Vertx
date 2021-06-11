package per.hynemankan.vertx.bilibot.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import lombok.extern.slf4j.Slf4j;
import per.hynemankan.vertx.bilibot.expection.StopPeriodicException;
import per.hynemankan.vertx.bilibot.expection.TryDoubleStartPeriodicException;
import per.hynemankan.vertx.bilibot.handlers.message.UnreadChecker;
import per.hynemankan.vertx.bilibot.utils.EventBusChannels;
import per.hynemankan.vertx.bilibot.utils.GlobalConstants;
@Slf4j
public class MessageFetchVerticle extends AbstractVerticle {
  private boolean messageFetchStatus=false;
  private Long timerId;
  private WebClient client;

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


  private void checkUnreadMessage(Long id){
    log.info("check unread message");
    UnreadChecker.getUnreadCount(client).onSuccess(res->{
      log.info(String.format("unread Check:%d", res));
      this.timerId = vertx.setTimer(GlobalConstants.MESSAGE_FETCH_PERIOD,this::checkUnreadMessage);
    }).onFailure(err->{
      log.warn(err.toString());
      this.timerId = vertx.setTimer(GlobalConstants.MESSAGE_FETCH_PERIOD,this::checkUnreadMessage);
    });
  }
}
