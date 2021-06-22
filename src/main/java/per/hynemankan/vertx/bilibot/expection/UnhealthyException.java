package per.hynemankan.vertx.bilibot.expection;

/**
 * unhealth exception
 *
 * @author hyneman
 */
public class UnhealthyException extends RuntimeException {

  private static final String MSG = "unhealthy!";

  public UnhealthyException() {
    super(MSG, new Throwable(MSG));
  }

  public String getMsg() {
    return MSG;
  }

}
