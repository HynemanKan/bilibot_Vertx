package per.hynemankan.vertx.bilibot.handlers.message;

import com.google.gson.JsonObject;
import io.vertx.core.Future;
import io.vertx.ext.web.client.WebClient;

public class MessageGetter {
  public static Future<JsonObject> getMessage(WebClient webClient) {
    return Future.future(response->{

    });
  }
}
