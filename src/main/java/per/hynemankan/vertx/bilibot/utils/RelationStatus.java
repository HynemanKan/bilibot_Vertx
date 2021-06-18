package per.hynemankan.vertx.bilibot.utils;

import io.vertx.core.json.JsonObject;

public enum RelationStatus {
  /**
   *
   */
  UNFOLLOW(0),
  FOLLOW(2),
  FOLLOW_DOUBLE(6),
  BLOCK(128),
  UNKNOWN(-1);

  private final Integer biliCode;

  public Integer getBiliCode(){
    return biliCode;
  }

  RelationStatus(Integer biliCode) {
    this.biliCode=biliCode;
  }

  public static RelationStatus getByCode(Integer biliCode) {
    for (RelationStatus relationStatus : RelationStatus.values()) {
      if (relationStatus.getBiliCode().equals(biliCode)) {
        return relationStatus;
      }
    }
    return RelationStatus.UNKNOWN;
  }

}
