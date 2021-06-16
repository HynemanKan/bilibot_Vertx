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

import java.net.MalformedURLException;
import java.net.URL;

public class UpdateAlreadyReadHandler {
  public static Future<Void> doUpdate(WebClient client, Integer talker, Integer messageIndex){
    return Future.future(response->{
      URL url;
      try {
        url = new URL(GlobalConstants.BILI_MESSAGE_UPDATE_ACK);
      } catch (MalformedURLException e) {
        response.fail(new WebClientException("Got illegal Url!", e));
        return;
      }
      HttpRequest<Buffer> request =client.post(GlobalConstants.BILI_PORT,url.getHost(),url.getPath());
      HeaderAdder.headerAdd(request);
      CookiesManager.headCookiesAdder(request)
        .onFailure(err->response.fail(err))
        .onSuccess(res->{
          String csrfToken = res.getJsonObject("bili_jct").getString("value");
          MultiMap formBody = MultiMap.caseInsensitiveMultiMap();
          formBody.add("talker_id",talker.toString())
            .add("session_type","1")
            .add("ack_seqno",messageIndex.toString())
            .add("build","0")
            .add("mobi_app","web")
            .add("csrf_token",csrfToken)
            .add("csrf",csrfToken);
          request.sendForm(formBody)
            .onFailure(err->response.fail(err))
            .onSuccess(httpResponse->{
              CookiesManager.updateCookiesFormHttpBody(httpResponse);
              JsonObject resJson = httpResponse.bodyAsJsonObject();
              if(resJson.getInteger("code")==0){
                response.complete();
              }else{
                response.fail(new MessageDealException("UpdateAlreadyReadHandler " + resJson.toString()));
              }
          });
      });
    });
  }
}
