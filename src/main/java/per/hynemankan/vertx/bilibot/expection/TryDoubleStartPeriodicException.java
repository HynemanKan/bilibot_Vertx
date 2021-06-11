package per.hynemankan.vertx.bilibot.expection;

public class TryDoubleStartPeriodicException extends RuntimeException {
  private static final String MSG = "double start period!";

  public TryDoubleStartPeriodicException() {
    super(MSG, new Throwable(MSG));
  }
}
