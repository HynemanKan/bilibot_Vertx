package per.hynemankan.vertx.bilibot.handlers.message;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.slf4j.Slf4j;
import per.hynemankan.vertx.bilibot.expection.WebClientException;
import per.hynemankan.vertx.bilibot.utils.CookiesManager;
import per.hynemankan.vertx.bilibot.utils.GlobalConstants;
import per.hynemankan.vertx.bilibot.utils.HeaderAdder;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author hyneman
 */
@Slf4j
public class UnreadChecker {
  public static Future<Integer> getUnreadCount(WebClient webClient) {
    return Future.future(response->{
      URL url;
      try {
        url = new URL(GlobalConstants.BILI_UNREAD_API);
      } catch (MalformedURLException e) {
        response.fail(new WebClientException("Got illegal Url!", e));
        return;
      }
      HttpRequest<Buffer> request = webClient.get(GlobalConstants.BILI_PORT,url.getHost(),url.getPath());
      HeaderAdder.headerAdd(request);
      CookiesManager.headCookiesAdder(request)
        .onFailure(err->response.fail(err)).onSuccess(res->{
          request.send().onFailure(err->response.fail(err))
            .onSuccess(httpResponse->{
              JsonObject resData = httpResponse.bodyAsJsonObject();
              if(!resData.getInteger("code").equals(0)){
                log.warn(resData.toString());
                response.fail(new WebClientException("webclinet error"));
              }
              JsonObject dataBody = resData.getJsonObject("data");
              Integer unreadCount= dataBody.getInteger("unfollow_unread")+dataBody.getInteger("follow_unread");
              response.complete(unreadCount);
            });
      });
    });
  }
}
