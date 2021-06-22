package per.hynemankan.vertx.bilibot.utils;

import io.vertx.core.json.JsonObject;

public enum CodeMapping {

  /**
   * json header
   */

  SUCCESS("0", "SUCCESS"),
  TRY_DOUBLE_LOGIN("1", "Login already"),
  UNKNOWN_ERROR("-1", "Unknown error"),
  REQUIRE_LOGIN("2", "Require login");
  /**
   * 返回码
   */
  private final String code;
  /**
   * 响应消息
   */
  private final String msg;

  CodeMapping(String code, String msg) {
    this.code = code;
    this.msg = msg;
  }

  public JsonObject toJson() {
    return new JsonObject()
      .put("code", this.code)
      .put("message", this.msg);
  }

  public static JsonObject successResponse(JsonObject data) {
    return CodeMapping.SUCCESS.toJson().put("data", data);
  }

  public String getMsg() {
    return msg;
  }

  public String getCode() {
    return code;
  }

}
