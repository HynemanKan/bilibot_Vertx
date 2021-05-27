package per.hynemankan.vertx.bilibot.handlers.common;

import io.vertx.core.Handler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExceptionHandler implements Handler<Throwable> {

  @Override
  public void handle(Throwable event) {
    //打印错误信息
    log.error("Exception:", event);
  }
}
