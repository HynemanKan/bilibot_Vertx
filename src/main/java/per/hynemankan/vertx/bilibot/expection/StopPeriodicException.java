package per.hynemankan.vertx.bilibot.expection;

public class StopPeriodicException extends RuntimeException {
  private static final String MSG = "stop period error!";

  public StopPeriodicException() {
    super(MSG, new Throwable(MSG));
  }
}
