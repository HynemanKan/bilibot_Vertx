package per.hynemankan.vertx.bilibot.handlers.info;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import per.hynemankan.vertx.bilibot.expection.WebClientException;
import per.hynemankan.vertx.bilibot.utils.CookiesManager;
import per.hynemankan.vertx.bilibot.utils.GlobalConstants;
import per.hynemankan.vertx.bilibot.utils.HeaderAdder;
import per.hynemankan.vertx.bilibot.utils.RelationStatus;

import java.net.MalformedURLException;
import java.net.URL;

public class GetRelation {
  public static Future<RelationStatus> getRelation(WebClient client, Integer uid) {
    return Future.future(response -> {
      URL url;
      try {
        url = new URL(GlobalConstants.BILI_INFO_RELATION_API);
      } catch (MalformedURLException e) {
        response.fail(new WebClientException("Got illegal Url!", e));
        return;
      }
      HttpRequest<Buffer> request = client.get(GlobalConstants.BILI_PORT, url.getHost(), url.getPath())
        .addQueryParam("fid", uid.toString());
      HeaderAdder.headerAdd(request);
      CookiesManager.headCookiesAdder(request)
        .onFailure(response::fail).onSuccess(res -> {
        request.send().onFailure(response::fail)
          .onSuccess(webResponse -> {
            JsonObject jsonBody = webResponse.bodyAsJsonObject();
            response.complete(RelationStatus.getByCode(jsonBody.getInteger("attribute")));
          });
      });
    });
  }
}
