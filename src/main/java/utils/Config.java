package utils;

import com.alibaba.fastjson.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;

public class Config {
    static final Logger LOGGER = LogManager.getLogger(Config.class);
    private static final Config instance = new Config();

    /**
     * 配置文件的文件名称
     */
    private final String filename = "config.json";

    /**
     * 从配置文件里读取到的JSON数据最终保存在这里
     */
    private JSONObject data;


    private Config() {
        load();
        init();
    }

    /**
     * 从配置文件里加载数据
     */
    private void load() {
        // 从根目录里加载 config.json 文件作为配置文件，并保存到 data 属性里
        FileInputStream fis;
        StringBuilder json = new StringBuilder();
        try {
            String path = System.getProperty("user.dir") + "/" + filename;
            fis = new FileInputStream(path);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            String line;
            try {
                while ( (line = br.readLine()) != null ) {
                    json.append(line);
                }
            } catch (IOException ignores) {
            }
        } catch (FileNotFoundException e) {
            LOGGER.info("执行目录下找不到配置文件 " + filename + "，将使用默认配置");
            json.append("{}");
        }
        data = JSONObject.parseObject(json.toString());
    }

    /**
     * 获取配置文件
     * @return String
     */
    public String getFilename() {
        return filename;
    }

    /**
     * 给 data 设置各种默认值
     */
    private void init() {
        JSONObject database = new JSONObject();
        if( data.containsKey("db") ) {
            database = data.getJSONObject("db");
        }
        // 如果不配置则默认使用SQLite
        if( !database.containsKey("type") ) {
            database.put("type", "SQLite");
        }
        // 如果用SQLite
        if( database.getString("type").equalsIgnoreCase("SQLite") ) {
            if( !database.containsKey("file") ) {
                database.put("file", "database.db");
            }
        }
        // 如果用MYSQL
        if( database.getString("type").equalsIgnoreCase("MYSQL") ) {
            if( !database.containsKey("host") ) {
                database.put("host", "127.0.0.1");
            }
            if( !database.containsKey("port") ) {
                database.put("port", 3306);
            }
            if( !database.containsKey("dbname") ) {
                database.put("dbname", "v_socket");
            }
            if( !database.containsKey("username") ) {
                database.put("username", "root");
            }
            if( !database.containsKey("password") ) {
                database.put("password", "");
            }
        }

        // 设置 socket 和 http 服务
        JSONObject socket = new JSONObject();
        JSONObject http = new JSONObject();
        if( data.containsKey("socket") ) {
            socket = data.getJSONObject("socket");
        }
        if( data.containsKey("http") ) {
            http = data.getJSONObject("http");
        }
        if( !socket.containsKey("port") ) {
            socket.put("port", 20100);
        }
        if( !http.containsKey("port") ) {
            http.put("port", 20101);
        }
        if( !socket.containsKey("timeout") ) {
            socket.put("timeout", 30);
        }

        JSONObject queue = new JSONObject();
        if( data.containsKey("message-queue") ) {
            queue = data.getJSONObject("message-queue");
        }
        if( !queue.containsKey("interval") ) {
            queue.put("interval", 1);
        }
        JSONObject security = new JSONObject();
        if( data.containsKey("security") ) {
            security = data.getJSONObject("security");
        }
        if( !security.containsKey("timestamp") ) {
            security.put("timestamp", true);
        }
        if( !security.containsKey("ssl") ) {
            security.put("ssl", new JSONObject());
        }
        JSONObject ssl = security.getJSONObject("ssl");
        if( !ssl.containsKey("enable") ) {
            ssl.put("enable", false);
        }
        if( !ssl.containsKey("certificate") ) {
            ssl.put("certificate", "certificate.pfx");
        }
        if( !ssl.containsKey("password") ) {
            ssl.put("password", "");
        }
        data.put("db", database);
        data.put("socket", socket);
        data.put("http", http);
        data.put("message-queue", queue);
        data.put("security", security);
    }

    /**
     * 获取配置实例化
     * @return Config
     */
    public static Config getInstance() {
        return instance;
    }

    /**
     * 获取数据库配置
     * @return JSONObject
     */
    public JSONObject getDatabase() {
        return data.getJSONObject("db");
    }

    /**
     * 获取数据库类型
     * @return String
     */
    public String getDatabaseType() {
        return this.getDatabase().getString("type");
    }

    /**
     * 获取HTTP服务配置
     * @return JSONObject
     */
    private JSONObject getHTTP() {
        return data.getJSONObject("http");
    }

    /**
     * 获取Socket服务配置
     * @return JSONObject
     */
    private JSONObject getSocket() {
        return data.getJSONObject("socket");
    }

    /**
     * 获取Socket服务端口号
     * @return int
     */
    public int getSocketPort() {
        return this.getSocket().getInteger("port");
    }

    /**
     * 获取Socket服务超时时间
     * @return int
     */
    public int getSocketTimeout() {
        return this.getSocket().getInteger("timeout");
    }

    /**
     * 获取HTTP服务端口号
     * @return int
     */
    public int getHTTPPort() {
        return this.getHTTP().getInteger("port");
    }

    /**
     * 获取消息处理队列配置
     * @return JSONObject
     */
    private JSONObject getMessageQueue() {
        return data.getJSONObject("message-queue");
    }

    /**
     * 获取消息处理队列的消费间隔（秒）
     * @return int
     */
    public int getQueueInterval() {
        return this.getMessageQueue().getInteger("interval");
    }

    /**
     * 获取服务安全配置
     * @return JSONObject
     */
    private JSONObject getSecurity() {
        return data.getJSONObject("security");
    }

    /**
     * 是否开启收到请求时校验时间戳有效期
     * 开启后接收到的请求内必须包含 timestamp 字段，并且对应的时间在服务器时间的正负600秒以内
     * 对于调试阶段可以关闭该校验，生产时建议打开
     * @return JSONObject
     */
    public boolean getEnableValidateTimestamp() {
        return this.getSecurity().getBoolean("timestamp");
    }

    /**
     * 获取SSL配置
     * @return JSONObject
     */
    private JSONObject getSSL() {
        return this.getSecurity().getJSONObject("ssl");
    }

    /**
     * 是否开启SSL，开启后连接需要以wss方式
     * @return boolean
     */
    public boolean getEnableSSL() {
        return this.getSSL().getBoolean("enable");
    }

    /**
     * 获取SSL证书文件的路径，一般情况下是一个格式的pfx文件
     * 当开启SSL时必填，否则服务报错
     * @return String
     */
    public String getSSLCertificatePath() {
        return this.getSSL().getString("certificate");
    }

    /**
     * 获取SSL证书密码
     * @return String
     */
    public String getSSLCertificatePassword() {
        return this.getSSL().getString("password");
    }
}
