package per.hynemankan.vertx.bilibot.utils;

import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpRequest;

import per.hynemankan.vertx.bilibot.utils.GlobalConstants;

public class HeaderAdder {
  public static void headerAdd(HttpRequest<Buffer> httpRequest){
    httpRequest.putHeader("User-Agent",GlobalConstants.REQUEST_HEADER);
  }
}
