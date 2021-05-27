package per.hynemankan.vertx.bilibot.utils;

public class GlobalConstants {
  protected GlobalConstants() {
  }
  public static final int HTTP_PORT = 8888;

  //redis key name
  public static final String RD_LOGIN_COOKIES="RD_LOGIN_COOKIES";

  //bilibili api url
  public static final String BILI_LOGIN_QRCODE_GET_API = "https://passport.bilibili.com/qrcode/getLoginUrl";
}
