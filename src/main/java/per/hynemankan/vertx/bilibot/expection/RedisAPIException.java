package per.hynemankan.vertx.bilibot.expection;

public class RedisAPIException extends RuntimeException {
  private static final String MSG = "redis error!";

  public RedisAPIException() {
    super(MSG, new Throwable(MSG));
  }

  public RedisAPIException(String msg, Throwable thrown) {
    super(msg, thrown);
  }
}
