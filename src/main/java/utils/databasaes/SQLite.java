package utils.databasaes;


import exceptions.DatabaseException;
import com.alibaba.fastjson.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utils.Config;

import java.sql.*;

public class SQLite extends DatabaseInterface {
    static final Logger LOGGER = LogManager.getLogger(Mysql.class);

    public SQLite() throws DatabaseException {
        super();
    }

    @Override
    String getDriver() {
        return "org.sqlite.JDBC";
    }

    @Override
    public boolean connect() throws DatabaseException {
        JSONObject config = Config.getInstance().getDatabase();
        String server = "jdbc:sqlite:" + config.getString("file");
        try {
            conn = DriverManager.getConnection(server);
            stm = conn.createStatement();
            initDB();
        } catch (SQLException e) {
            LOGGER.error("连接SQLite出错：" + e.getMessage());
            throw new DatabaseException("无法连接SQLite");
        }
        return true;
    }

    /**
     * 初始化数据库
     */
    public void initDB() {
        // 如果 certificate 表不存在则创建
        try {
            JSONObject model = this.one("SELECT * FROM sqlite_master WHERE name = 'certificate' LIMIT 1;");
            if( model.isEmpty() ) {
                String sql = "CREATE TABLE \"certificate\" (\n" +
                        "  \"appid\" text(32) NOT NULL,\n" +
                        "  \"private\" TEXT(2048) NOT NULL,\n" +
                        "  \"public\" TEXT(2048) NOT NULL,\n" +
                        "  \"remark\" TEXT(255),\n" +
                        "  PRIMARY KEY (\"appid\")\n" +
                        ");";
                stm.execute(sql);
            }
        } catch (DatabaseException | SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
