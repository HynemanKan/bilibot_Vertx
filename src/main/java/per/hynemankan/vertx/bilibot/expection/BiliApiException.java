package per.hynemankan.vertx.bilibot.expection;

public class BiliApiException extends RuntimeException {
  public BiliApiException(String msg) {
    super(msg, new Throwable(msg));
  }
}
