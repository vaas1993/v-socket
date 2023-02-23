package utils.databasaes;


import exceptions.DatabaseException;
import com.alibaba.fastjson.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utils.Config;

import java.sql.*;

public class Mysql extends DatabaseInterface {
    static final Logger LOGGER = LogManager.getLogger(Mysql.class);

    public Mysql() throws DatabaseException {
        super();
    }

    @Override
    String getDriver() {
        return "com.mysql.cj.jdbc.Driver";
    }

    @Override
    public boolean connect() throws DatabaseException {
        JSONObject config = Config.getInstance().getDatabase();
        String server = "jdbc:mysql://" + config.getString("host") + ":" + config.getString("port") + "/" + config.getString("dbname");
        try {
            conn = DriverManager.getConnection(server, config.getString("username"), config.getString("password"));
            stm = conn.createStatement();
        } catch (SQLException e) {
            LOGGER.error("连接MYSQL出错：" + e.getMessage());
            throw new DatabaseException("无法连接MYSQL");
        }
        return true;
    }
}
