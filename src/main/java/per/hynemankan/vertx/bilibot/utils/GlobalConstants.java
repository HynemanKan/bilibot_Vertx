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
  public static final Integer RD_LOGIN_COOKIES_TIMEOUT=3600;//EX
  public static final String STATIC_PATH="C:\\Users\\hyneman\\Desktop\\project\\personal\\bilibot_Vertx\\staticPage";
  public static final String COOKIES_LOCK = "BILI_COOKIES_LOCK";
  //web client
  public static final Integer BILI_PORT =80;
  public static final String REQUEST_HEADER =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.212 Safari/537.36 Edg/90.0.818.66";
  //bilibili api url
  public static final String BILI_LOGIN_QRCODE_GET_API = "https://passport.bilibili.com/qrcode/getLoginUrl";
  public static final String BILI_LOGIN_STATUS_API="https://passport.bilibili.com/qrcode/getLoginInfo";
  public static final String BILI_INFO_BASE_INFO_API ="https://api.bilibili.com/x/web-interface/nav";
  public static final String BILI_UNREAD_API="https://api.vc.bilibili.com/session_svr/v1/session_svr/single_unread";
  public static final String BILI_MESSAGE_GET_API="https://api.vc.bilibili.com/session_svr/v1/session_svr/get_sessions";
  //message fetch
  public static final Integer MESSAGE_FETCH_PERIOD=1500;
}
