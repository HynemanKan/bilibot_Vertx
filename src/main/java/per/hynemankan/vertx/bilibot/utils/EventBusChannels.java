package per.hynemankan.vertx.bilibot.utils;

/**
 * 定义eventBus channels
 *
 * @author hyneman
 * @create 2021-05-27
 */
public enum EventBusChannels {
  // 配置变更
  CONFIGURATION_CHANGED,
  //redis setting
  SET_REDIS_OPTIONS,
  GET_DISTRIBUTED_LOCK,
  RELEASE_DISTRIBUTED_LOCK,
  REDIS_CONNECT,
  //mysql setting
  SET_MYSQL_OPTIONS,
  INIT_MYSQL,
  //messageFetch
  START_MESSAGE_FETCH,
  END_MESSAGE_FETCH,
  RAISE_SESSION

}
