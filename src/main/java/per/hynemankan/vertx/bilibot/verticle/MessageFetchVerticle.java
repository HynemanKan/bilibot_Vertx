package per.hynemankan.vertx.bilibot.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import per.hynemankan.vertx.bilibot.expection.StopPeriodicException;
import per.hynemankan.vertx.bilibot.expection.TryDoubleStartPeriodicException;
import per.hynemankan.vertx.bilibot.utils.EventBusChannels;
import per.hynemankan.vertx.bilibot.utils.GlobalConstants;
@Slf4j
public class MessageFetchVerticle extends AbstractVerticle {
  private boolean messageFetchStatus=false;
  private Long timerId;

  @Override
  public void start(Promise<Void> startPromise) {

    vertx.eventBus().<String>consumer(EventBusChannels.START_MESSAGE_FETCH.name()).handler(this::startMessageFetch);
    vertx.eventBus().<String>consumer(EventBusChannels.END_MESSAGE_FETCH.name()).handler(this::endMessageFetch);
    log.info("Init message fetch success!");
    startPromise.complete();
  }

  private void startMessageFetch(Message<String> message){
    if(!messageFetchStatus){
      this.timerId = vertx.setPeriodic(GlobalConstants.MESSAGE_FETCH_PERIOD,this::checkUnreadMessage);
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
  }
}
