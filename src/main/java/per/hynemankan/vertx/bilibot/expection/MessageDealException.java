package per.hynemankan.vertx.bilibot.expection;

public class MessageDealException extends RuntimeException{
  public MessageDealException(String msg) {
    super(msg, new Throwable(msg));
  }
}
