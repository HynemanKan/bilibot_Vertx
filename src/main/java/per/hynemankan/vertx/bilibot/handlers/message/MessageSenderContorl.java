package per.hynemankan.vertx.bilibot.handlers.message;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.slf4j.Slf4j;
import per.hynemankan.vertx.bilibot.expection.MessageStackFullException;
import per.hynemankan.vertx.bilibot.utils.GlobalConstants;
import per.hynemankan.vertx.bilibot.utils.MessageType;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Stack;
@Slf4j
public class MessageSenderContorl {
  private Queue<JsonObject> tasks;
  private final WebClient client;
  private final Integer maxTaskNum;
  private final Vertx vertx;
  public MessageSenderContorl(Vertx vertx, WebClient client){
    tasks =new ArrayDeque<>();
    this.client = client;
    this.maxTaskNum = GlobalConstants.MAX_TASK_NUM;
    this.vertx = vertx;
  }

  public void startTimer(){
    vertx.setTimer(GlobalConstants.MESSAGE_SEND_PERIOD,this::sendMessageLoop);
  }

  private void sendMessageLoop(long timerId){
    if(!tasks.isEmpty()){
      log.info(String.format("get send message task, total %d", tasks.size()));
      JsonObject task = tasks.poll();
      switch(MessageType.valueOf(task.getString(GlobalConstants.MESSAGE_TYPE))){
        case TEXT:
          MessageSender.sendTextMessage(
            client,
            task.getString("content"),
            task.getInteger(GlobalConstants.SENDER),
            task.getInteger(GlobalConstants.RECEIVER)
            ).onFailure(err->{
              log.info(err.getMessage());
              vertx.setTimer(GlobalConstants.MESSAGE_SEND_PERIOD,this::sendMessageLoop);
            }).onSuccess(res->{
              log.info("Message send success");
              vertx.setTimer(GlobalConstants.MESSAGE_SEND_PERIOD,this::sendMessageLoop);
          });
          break;
        case IMAGE:
          break;
        default:
      }
    }else{
      log.info("Stack Empty keep waiting");
      vertx.setTimer(GlobalConstants.MESSAGE_SEND_PERIOD,this::sendMessageLoop);
    }
  }

  public void sendTextMessage(String content,Integer sender,Integer receiver){
   JsonObject taskBody = new JsonObject()
     .put(GlobalConstants.MESSAGE_TYPE, MessageType.TEXT.name())
     .put(GlobalConstants.SENDER,sender)
     .put(GlobalConstants.RECEIVER,receiver)
     .put("content",content);
   if(tasks.size()<maxTaskNum){
     tasks.add(taskBody);
   }else{
     throw new MessageStackFullException();
   }
  }
}
