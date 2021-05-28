package per.hynemankan.vertx.bilibot.utils;

import io.vertx.core.json.JsonObject;

public enum CodeMapping {

  /**
   * json header
   */

  SUCCESS("0","SUCCESS"),
  TRY_DOUBLE_LOGIN("1","login already"),
  UNKNOWN_ERROR("-1","unknown error");
  /**
   * 返回码
   */
  private final String code;
  /**
   * 响应消息
   */
  private final String msg;

  CodeMapping(String code,String msg){
    this.code = code;
    this.msg = msg;
  }

  public JsonObject toJson() {
    return new JsonObject()
      .put("code", this.code)
      .put("message", this.msg);
  }

  public String getMsg() {
    return msg;
  }

  public String getCode() { return code; }

}
