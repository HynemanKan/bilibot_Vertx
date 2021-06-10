package per.hynemankan.vertx.bilibot.expection;

public class UnloginException extends RuntimeException{
  private static final String MSG = "Require login!";

  public UnloginException(){
    super(MSG, new Throwable(MSG));
  }
}
