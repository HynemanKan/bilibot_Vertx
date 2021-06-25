package per.hynemankan.vertx.bilibot.handlers.message;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.slf4j.Slf4j;
import per.hynemankan.vertx.bilibot.expection.MessageStackFullException;
import per.hynemankan.vertx.bilibot.handlers.info.GetBaseInfo;
import per.hynemankan.vertx.bilibot.handlers.support.ImageUpload;
import per.hynemankan.vertx.bilibot.utils.GlobalConstants;
import per.hynemankan.vertx.bilibot.utils.MessageType;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * 消息发送队列
 *
 * @author hyneman
 */
@Slf4j
public class MessageSenderContorl {
  private Queue<JsonObject> tasks;
  private final WebClient client;
  private final Integer maxTaskNum;
  private final Vertx vertx;
  private Integer selfId=0;

  public MessageSenderContorl(Vertx vertx, WebClient client) {
    tasks = new ArrayDeque<>();
    this.client = client;
    this.maxTaskNum = GlobalConstants.MAX_TASK_NUM;
    this.vertx = vertx;
  }

  public Future<Void> setSelfId(){
    return Future.future(res->{
      GetBaseInfo.getBaseInfo(client)
        .onFailure(res::fail).onSuccess(httpResponse->{
          this.selfId=httpResponse.getInteger("mid");
          res.complete();
      });
    });
  }

  public void startTimer() {
    vertx.setTimer(GlobalConstants.MESSAGE_SEND_PERIOD, this::sendMessageLoop);
  }

  private void sendMessageLoop(long timerId) {
    if ((!tasks.isEmpty()) && selfId!=0) {
      log.info(String.format("get send message task, total %d", tasks.size()));
      JsonObject task = tasks.poll();
      switch (MessageType.valueOf(task.getString(GlobalConstants.MESSAGE_TYPE))) {
        case TEXT:
          MessageSender.sendTextMessage(
            client,
            task.getString("content"),
            task.getInteger(GlobalConstants.SENDER),
            task.getInteger(GlobalConstants.RECEIVER)
          ).onFailure(err -> {
            log.info(err.getMessage());
            vertx.setTimer(GlobalConstants.MESSAGE_SEND_PERIOD, this::sendMessageLoop);
          }).onSuccess(res -> {
            log.info("Message send success");
            vertx.setTimer(GlobalConstants.MESSAGE_SEND_PERIOD, this::sendMessageLoop);
          });
          break;
        case IMAGE:
          MessageSender.sendImageMessage(
            client,
            task.getString("imageUrl"),
            task.getInteger("width"),
            task.getInteger("height"),
            task.getInteger(GlobalConstants.SENDER),
            task.getInteger(GlobalConstants.RECEIVER)
          ).onFailure(err -> {
            log.info(err.getMessage());
            vertx.setTimer(GlobalConstants.MESSAGE_SEND_PERIOD, this::sendMessageLoop);
          }).onSuccess(res -> {
            log.info("Message send success");
            vertx.setTimer(GlobalConstants.MESSAGE_SEND_PERIOD, this::sendMessageLoop);
          });
        default:
      }
    } else {
      log.info("keep waiting");
      vertx.setTimer(GlobalConstants.MESSAGE_SEND_PERIOD, this::sendMessageLoop);
    }
  }

  public void sendTextMessage(String content, Integer receiver) {
    JsonObject taskBody = new JsonObject()
      .put(GlobalConstants.MESSAGE_TYPE, MessageType.TEXT.name())
      .put(GlobalConstants.SENDER, selfId)
      .put(GlobalConstants.RECEIVER, receiver)
      .put("content", content);
    if (tasks.size() < maxTaskNum) {
      tasks.add(taskBody);
    } else {
      throw new MessageStackFullException();
    }
  }

  public void sendImageMessage(String imageUrl,Integer width,Integer height, Integer receiver){
    JsonObject taskBody = new JsonObject()
      .put(GlobalConstants.MESSAGE_TYPE, MessageType.IMAGE.name())
      .put(GlobalConstants.SENDER, selfId)
      .put(GlobalConstants.RECEIVER, receiver)
      .put("imageUrl", imageUrl)
      .put("width",width)
      .put("height",height);
    if (tasks.size() < maxTaskNum) {
      tasks.add(taskBody);
    } else {
      throw new MessageStackFullException();
    }
  }

  public void sendLocalImageMessage(String filePath, Integer receiver){
    ImageUpload.puloadImageByPath(client, filePath, "test.jpg")
      .onFailure(err->log.warn(err.getMessage())).onSuccess(res -> {
        sendImageMessage(
          res.getString("image_url"),
          res.getInteger("image_width"),
          res.getInteger("image_height"),
          receiver);
    });
  }
}
