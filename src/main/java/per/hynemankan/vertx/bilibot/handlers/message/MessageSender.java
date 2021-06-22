package per.hynemankan.vertx.bilibot.handlers.message;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import per.hynemankan.vertx.bilibot.expection.MessageDealException;
import per.hynemankan.vertx.bilibot.expection.WebClientException;
import per.hynemankan.vertx.bilibot.utils.CookiesManager;
import per.hynemankan.vertx.bilibot.utils.GlobalConstants;
import per.hynemankan.vertx.bilibot.utils.HeaderAdder;
import per.hynemankan.vertx.bilibot.utils.MessageType;

import java.net.MalformedURLException;
import java.net.URL;


/**
 * ！！！请勿直接使用该方法！！！
 *
 * @author hyneman
 */
public class MessageSender {
  private static final String DEVICE_ID_MODEL = "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx";

  private static String genDeviceId() {
    Integer num;
    double random;
    String deviceId = "";
    char[] c = DEVICE_ID_MODEL.toCharArray();
    for (int i = 0; i < c.length; i++) {
      random = Math.floor(16 * Math.random());
      num = Integer.valueOf((int) random);
      switch (c[i]) {
        case 'x':
          deviceId = deviceId + Integer.toHexString(num);
          break;
        case 'y':
          num = 3 & num | 8;
          deviceId = deviceId + Integer.toHexString(num);
          break;
        default:
          deviceId = deviceId + c[i];
      }
    }
    return deviceId;
  }

  public  static Future<Void> sendMessage(WebClient webClient, JsonObject messageBody, Integer messageType, Integer sender, Integer receiver){
    return Future.future(response -> {
      URL url;
      try {
        url = new URL(GlobalConstants.BILI_MESSAGE_SEND_API);
      } catch (MalformedURLException e) {
        response.fail(new WebClientException("Got illegal Url!", e));
        return;
      }
      HttpRequest<Buffer> request = webClient.post(GlobalConstants.BILI_PORT, url.getHost(), url.getPath());
      HeaderAdder.headerAdd(request);
      CookiesManager.headCookiesAdder(request)
        .onFailure(err -> response.fail(err)).onSuccess(res -> {
        MultiMap formData = MultiMap.caseInsensitiveMultiMap();
        long timeStamp = System.currentTimeMillis() / 1000;
        String csrfToken = res.getJsonObject("bili_jct").getString("value");
        formData.set("msg[sender_uid]", sender.toString())
          .set("msg[receiver_id]", receiver.toString())
          .set("msg[receiver_type]", "1")
          .set("msg[msg_type]", messageType.toString())
          .set("msg[msg_status]", "0")
          .set("msg[timestamp]", String.valueOf(timeStamp))
          .set("msg[new_face_version]", "0")
          .set("msg[dev_id]", genDeviceId())
          .set("from_firework", "0")
          .set("build", "0")
          .set("mobi_app", "web")
          .set("csrf_token", csrfToken)
          .set("csrf", csrfToken)
          .set("msg[content]", messageBody.toString());
        request.sendForm(formData)
          .onFailure(err -> response.fail(err)).onSuccess(httpResponse -> {
          CookiesManager.updateCookiesFormHttpBody(httpResponse);
          JsonObject resJson = httpResponse.bodyAsJsonObject();
          if (resJson.getInteger("code") == 0) {
            response.complete();
          } else {
            response.fail(new MessageDealException("Message sender fail" + resJson.toString()));
          }
        });
      });
    });
  }

  public static Future<Void> sendTextMessage(WebClient webClient, String content, Integer sender, Integer receiver) {
    JsonObject messageBody = new JsonObject();
    messageBody.put("content", content);
    return sendMessage(webClient,messageBody,1,sender,receiver);
  }

  public static Future<Void> sendImageMessage(WebClient webClient, String imageUrl, Integer sender, Integer receiver){
    JsonObject messageBody = new JsonObject();
    messageBody.put("url", imageUrl);
    return sendMessage(webClient,messageBody,2,sender,receiver);
  }

}
