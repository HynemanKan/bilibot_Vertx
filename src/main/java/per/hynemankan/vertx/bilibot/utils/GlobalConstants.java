package per.hynemankan.vertx.bilibot.utils;

public class GlobalConstants {
  protected GlobalConstants() {
  }
  public static final int HTTP_PORT = 8888;

  //redis
  public static final String TIME_S_MARK = "EX";
  public static final String TIME_MS_MARK = "PX";
  //redis key name
  public static final String RD_LOGIN_COOKIES="RD_LOGIN_COOKIES";
  public static final String RD_LOGIN_OAUTHKEY="RD_LOGIN_OAUTHKEY";
  public static final Integer RD_LOGIN_OAUTHKEY_TIMEOUT=180;//EX


  //bilibili api url
  public static final String REQUEST_HEADER =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.212 Safari/537.36 Edg/90.0.818.66";

  public static final String BILI_LOGIN_QRCODE_GET_API = "https://passport.bilibili.com/qrcode/getLoginUrl";
}
