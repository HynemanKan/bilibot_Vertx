package per.hynemankan.vertx.bilibot.handlers.message;


import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
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

@Slf4j
public class MessageGetter {
  public static Future<JsonObject> getMessage(WebClient webClient,Long lastFetchTimestamp) {
    return Future.future(response->{
      URL url;
      try {
        url = new URL(GlobalConstants.BILI_MESSAGE_GET_API);
      } catch (MalformedURLException e) {
        response.fail(new WebClientException("Got illegal Url!", e));
        return;
      }
      HttpRequest<Buffer> request = webClient.get(GlobalConstants.BILI_PORT,url.getHost(),url.getPath())
        .addQueryParam("mobi_app","web")
        .addQueryParam("build","0")
        .addQueryParam("begin_ts",lastFetchTimestamp.toString());
      HeaderAdder.headerAdd(request);
      CookiesManager.headCookiesAdder(request)
        .onFailure(err->response.fail(err)).onSuccess(res->{
          request.send().onFailure(err-> response.fail(err)).onSuccess(httpResponse->{
            JsonObject resJson = httpResponse.bodyAsJsonObject();
            if(!resJson.getInteger("code").equals(0)){
              log.info(resJson.toString());
              response.fail(new WebClientException("get message api response error"));
              return;
            }
            JsonObject JsonBody = resJson.getJsonObject("data");
            JsonObject outJson = new JsonObject();
            if (!JsonBody.containsKey("session_list")){
              outJson.put("count",0);
              response.complete(outJson);
              return;
            }
            outJson.put("count",JsonBody.getJsonArray("session_list").size());
            outJson.put("session_list",JsonBody.getJsonArray("session_list"));
            response.complete(outJson);
            return;
          });
      });
    });
  }
}
