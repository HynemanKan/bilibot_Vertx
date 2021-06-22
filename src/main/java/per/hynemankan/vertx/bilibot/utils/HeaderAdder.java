package per.hynemankan.vertx.bilibot.utils;

import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpRequest;

import io.vertx.ext.web.client.predicate.ResponsePredicate;
import per.hynemankan.vertx.bilibot.utils.GlobalConstants;

public class HeaderAdder {
  public static void headerAdd(HttpRequest<Buffer> httpRequest) {
    httpRequest.headers().set("user-agent", GlobalConstants.REQUEST_HEADER);
    httpRequest.timeout(GlobalConstants.REQUEST_TIMEOUT);
    httpRequest.ssl(GlobalConstants.USE_SSL);
    httpRequest.expect(ResponsePredicate.status(200));
  }
}
