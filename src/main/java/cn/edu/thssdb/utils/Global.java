package cn.edu.thssdb.utils;

public class Global {
  public static int fanout = 129; // original: 129

  public static int SUCCESS_CODE = 0;
  public static int FAILURE_CODE = -1;

  public static String DEFAULT_SERVER_HOST = "127.0.0.1";
  public static int DEFAULT_SERVER_PORT = 6667;
  public static String DEFAULT_USER_NAME = "root";
  public static String DEFAULT_PASSWORD = "root";

  public static String CLI_PREFIX = "ThssDB2023>";
  public static final String SHOW_TIME = "show time;";
  public static final String CONNECT = "connect";
  public static final String DISCONNECT = "disconnect;";
  public static final String QUIT = "quit;";

  public static final String S_URL_INTERNAL = "jdbc:default:connection";

  public static final String DATA_DIR = "data/";

  public static final int CACHE_SIZE = 4; // maximum cache size of node manager

  public static final int COMP_EQ = 0; // =
  public static final int COMP_GE = 1; // >=
  public static final int COMP_GT = 2; // >
  public static final int COMP_LE = 3; // <=
  public static final int COMP_LT = 4; // <
  public static final int COMP_NE = 5; // <>

  // node types
  public static final int INTERNAL = 0;
  public static final int LEAF = 1;

  public static final int INITIAL_LRU = 10;

  public enum ISOLATION_LEVEL {
    READ_UNCOMMITTED,
    READ_COMMITTED,
    SERIALIZABLE
  }

  public static ISOLATION_LEVEL DATABASE_ISOLATION_LEVEL = ISOLATION_LEVEL.READ_COMMITTED;
  public static final boolean ISOLATION_STATUS = true;
}
