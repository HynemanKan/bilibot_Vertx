package per.hynemankan.vertx.bilibot.expection;

public class SessionBusyException extends RuntimeException{
  private static final String MSG = "Session busy";

  public SessionBusyException(){
    super(MSG, new Throwable(MSG));
  }
}
