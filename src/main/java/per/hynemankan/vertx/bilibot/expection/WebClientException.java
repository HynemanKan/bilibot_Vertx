package per.hynemankan.vertx.bilibot.expection;

public class WebClientException extends RuntimeException {
  public WebClientException(String msg) {
    super(msg, new Throwable(msg));
  }

  public WebClientException(String msg, Throwable thrown) {
    super(msg, thrown);
  }
}
