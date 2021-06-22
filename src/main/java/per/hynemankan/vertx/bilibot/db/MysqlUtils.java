package per.hynemankan.vertx.bilibot.db;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.PoolOptions;
import lombok.extern.slf4j.Slf4j;

import per.hynemankan.vertx.bilibot.utils.EventBusChannels;
import per.hynemankan.vertx.bilibot.utils.ConfigurationKeys;

@Slf4j
public class MysqlUtils extends AbstractVerticle {
  private static MySQLPool client;

  /**
   * MySQL主机
   */
  private String host;

  /**
   * MySQL端口
   */
  private int port;

  /**
   * MySQL用户名
   */
  private String username;

  /**
   * MySQL密码
   */
  private String password;

  /**
   * MySQL数据库
   */
  private String database;

  /**
   * MySQL timeout
   */
  private int timeOut;

  /**
   * MySQL连接池最大链接数量
   */
  private int maxPoolSize;

  @Override
  public void start(Promise<Void> startPromise) {

    vertx.eventBus().<JsonObject>consumer(EventBusChannels.SET_MYSQL_OPTIONS.name()).handler(this::setMysqlOptions);
    vertx.eventBus().<String>consumer(EventBusChannels.INIT_MYSQL.name()).handler(this::initMysql);
    vertx.eventBus().request(EventBusChannels.SET_MYSQL_OPTIONS.name(), config().getJsonObject("mysql"), r -> {
      if (r.succeeded()) {
        startPromise.complete();
      } else {
        startPromise.fail("Mysql init failed!");
      }
    });
  }

  /**
   * get client
   */
  public static MySQLPool getMysqlClient() {
    return client;
  }

  private static void setClient(MySQLPool client) {
    MysqlUtils.client = client;
  }

  /**
   * 初始化MySQL连接
   *
   * @param message 连接MySQL的json信息（包括 主机，端口，数据库，用户名，密码）
   */
  private void setMysqlOptions(Message<JsonObject> message) {
    // 从json中拿到连接相关信息
    int portInput = message.body().getInteger(ConfigurationKeys.MYSQL_PORT.name());
    int maxPoolSizeInput = message.body().getInteger(ConfigurationKeys.MYSQL_MAX_POOL_SIZE.name());
    String hostInput = message.body().getString(ConfigurationKeys.MYSQL_ADDRESS.name());
    String usernameInput = message.body().getString(ConfigurationKeys.MYSQL_USERNAME.name());
    String passwordInput = message.body().getString(ConfigurationKeys.MYSQL_PASSWORD.name());
    String databaseInput = message.body().getString(ConfigurationKeys.MYSQL_DATABASE.name());
    int timeoutInput = message.body().getInteger(ConfigurationKeys.MYSQL_TIMEOUT.name());

    // 如果MySQL配置文件有更改
    if (!hostInput.equals(host)
      || portInput != port
      || !usernameInput.equals(username)
      || !passwordInput.equals(password)
      || !databaseInput.equals(database)
      || maxPoolSizeInput != maxPoolSize
      || timeoutInput != timeOut
    ) {

      // 则对其进行更改
      host = hostInput;
      port = portInput;
      username = usernameInput;
      password = passwordInput;
      database = databaseInput;
      maxPoolSize = maxPoolSizeInput;
      timeOut = timeoutInput;

      vertx.eventBus().request(EventBusChannels.INIT_MYSQL.name(),
        "connect", rm -> message.reply("success"));
    }
  }

  private void initMysql(Message<String> message) {
    try {
      // 将连接信息封装到connectOptions
      MySQLConnectOptions connectOptions = new MySQLConnectOptions()
        .setHost(host)
        .setPort(port)
        .setUser(username)
        .setPassword(password)
        .setDatabase(database)
        .setIdleTimeout(timeOut);

      // 连接池设置
      PoolOptions poolOptions = new PoolOptions().setMaxSize(maxPoolSize);

      // 创建MySQL client
      setClient(MySQLPool.pool(vertx, connectOptions, poolOptions));

      // MySQL连接校验
      getMysqlClient()
        .query("select 1")
        .execute(ar -> {
          if (ar.succeeded()) {
            log.info("Init Mysql success!");
            message.reply(true);
          } else {
            message.reply(false);
            throw new RuntimeException("Init mysql error: ", ar.cause());
          }
        });

    } catch (Exception e) {
      message.reply(false);
      try {
        throw new RuntimeException("Init mysql error: " + e.getMessage());
      } catch (Exception ex) {
        throw new RuntimeException("throwable", ex.getCause());
      }
    }
  }
}
