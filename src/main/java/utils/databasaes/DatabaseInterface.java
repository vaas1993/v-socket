package utils.databasaes;

import exceptions.DatabaseException;
import com.alibaba.fastjson.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

abstract public class DatabaseInterface {
    static final Logger LOGGER = LogManager.getLogger(DatabaseInterface.class);
    /**
     * 数据库连接
     */
    public Connection conn;

    /**
     * SQL语句执行对象
     */
    public Statement stm;

    /**
     * 返回驱动名
     * @return String
     */
    abstract String getDriver();

    /**
     * 获取一条数据
     * @param sql String
     * @return JSONObject
     */
    public JSONObject one(String sql) throws DatabaseException {
        if( getIsDisconnect() ) {
            connect();
        }
        JSONObject model = new JSONObject();
        try {
            ResultSet result = stm.executeQuery(sql);
            int columnCount = result.getMetaData().getColumnCount();
            if( result.next() ) {
                // 这里注意一下 column 是从 1 开始的，不是 0
                for(int i = 1; i <= columnCount; i++) {
                    model.put(result.getMetaData().getColumnName(i), result.getString(i));
                }
            }
        } catch (SQLException e) {
            LOGGER.error("执行SQL出错：" + e.getMessage());
            throw new DatabaseException(e.getMessage());
        }
        return model;
    }

    /**
     * 连接数据库
     * @return boolean
     */
    abstract boolean connect() throws DatabaseException;

    public DatabaseInterface() throws DatabaseException {
        try {
            loadDriver();
        } catch (DatabaseException e) {
            LOGGER.error("加载JDBC驱动失败：" + e.getMessage());
            throw new DatabaseException(e.getMessage());
        }
    }

    /**
     * 加载驱动
     */
    public void loadDriver() throws DatabaseException {
        // 加载JDBC
        try {
            Class.forName(this.getDriver());
        } catch (ClassNotFoundException e) {
            LOGGER.error("JDBC驱动加载失败：" + e.getMessage());
            throw new DatabaseException(e.getMessage());
        }
        // 连接数据库
        connect();
    }

    /**
     * 获取当前连接是否断开
     * @return boolean
     */
    public boolean getIsDisconnect() throws DatabaseException {
        try {
            if( conn != null && !conn.isClosed() ) {
                return false;
            }
        } catch (SQLException e) {
            LOGGER.error("获取数据库连接状态出错：" + e.getMessage());
            throw new DatabaseException(e.getMessage());
        }
        return true;
    }
}