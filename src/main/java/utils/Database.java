package utils;

import exceptions.DatabaseException;
import exceptions.ProcessException;
import com.alibaba.fastjson.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utils.databasaes.DatabaseInterface;
import utils.databasaes.Mysql;
import utils.databasaes.SQLite;

import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Database {
    private static final Logger LOGGER = LogManager.getLogger(Database.class);

    private static final Database instance = new Database();

    private DatabaseInterface database;

    private ConcurrentMap<String, JSONObject> certificateList = new ConcurrentHashMap<>();

    private Database() {
        try {
            if (Config.getInstance().getDatabaseType().equalsIgnoreCase("SQLite")) {
                database = new SQLite();
            } else {
                database = new Mysql();
            }
        } catch (DatabaseException e) {
            throw new RuntimeException(e);
        }
    }

    public static Database getInstance() {
        return instance;
    }

    /**
     * 根据应用ID从数据库中取出对应的数据行
     *
     * @param appid String
     * @return JSONObject
     */
    public JSONObject getCertificate(String appid) throws ProcessException {
        if (!certificateList.containsKey(appid)) {
            JSONObject instance;
            try {
                instance = database.one("SELECT * FROM certificate WHERE appid = '" + appid + "' LIMIT 1;");
            } catch (DatabaseException e) {
                LOGGER.error("使用应用ID查询证书出错：" + e.getMessage());
                throw new ProcessException(e.getMessage());
            }
            if (instance == null || instance.isEmpty()) {
                throw new ProcessException("appid 不存在：" + appid, 10000);
            }
            certificateList.put(appid, instance);
        }
        return certificateList.get(appid);
    }

    public void createCertificate(String appid, String publicKey, String privateKey, String remark) throws SQLException {
        String sql = "INSERT INTO certificate(`appid`,`public`,`private`,`remark`) VALUES('"+appid+"','"+publicKey+"','"+privateKey+"','"+remark+"')";
        database.stm.execute(sql);
    }
}