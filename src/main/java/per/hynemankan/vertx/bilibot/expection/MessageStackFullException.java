package per.hynemankan.vertx.bilibot.expection;

public class MessageStackFullException extends RuntimeException {
  private static final String MSG = "Message stack full!";

  public MessageStackFullException() {
    super(MSG, new Throwable(MSG));
  }
}
