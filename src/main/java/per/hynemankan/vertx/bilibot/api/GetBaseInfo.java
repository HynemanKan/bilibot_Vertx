package per.hynemankan.vertx.bilibot.api;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.slf4j.Slf4j;
import per.hynemankan.vertx.bilibot.expection.UnloginException;
import per.hynemankan.vertx.bilibot.expection.WebClientException;
import per.hynemankan.vertx.bilibot.utils.CookiesManager;
import per.hynemankan.vertx.bilibot.utils.GlobalConstants;
import per.hynemankan.vertx.bilibot.utils.HeaderAdder;

import java.net.MalformedURLException;
import java.net.URL;
@Slf4j
public class GetBaseInfo {
  public static Future<JsonObject> getBaseInfo(WebClient webClient){
    return Future.future(res->{
      URL url;
      try {
        url = new URL(GlobalConstants.BILI_INFO_BASE_INFO_API);
      } catch (MalformedURLException e) {
        res.fail(new WebClientException("Got illegal Url!", e));
        return;
      }
      HttpRequest<Buffer> request = webClient.get(GlobalConstants.BILI_PORT, url.getHost(), url.getPath());
      HeaderAdder.headerAdd(request);
      CookiesManager.headCookiesAdder(request).onSuccess(ar->{
        log.info(request.headers().toString());
        request.send().onSuccess(response->{
          JsonObject jsonBody=  response.bodyAsJsonObject();
          if(jsonBody.getInteger("code")==-101){
            res.fail(new UnloginException());
          }else{
            res.complete(jsonBody.getJsonObject("data"));
          }
        }).onFailure(response->{
          res.fail(new WebClientException("webclient error"));
        });
      }).onFailure(ar->{
        res.fail(ar.getCause());
      });
    });
  }
}
