package services.actions.sockets;

import channel.ChannelManager;
import exceptions.ProcessException;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import utils.Config;
import utils.RSA;

public class LoginAction extends BaseSocketAction {
    /**
     * 破译后的原文
     */
    JSONObject decipher;

    @Override
    public JSONObject run() {
        // 存储用户信息和连接信息
        ChannelManager cm = ChannelManager.getInstance();
        cm.add(params.getString("appid"), decipher.getString("id"), channel);
        return new JSONObject();
    }

    @Override
    public void beforeRun() throws ProcessException {
        if( !params.containsKey("appid") ) {
            throw new ProcessException("appid不能为空", 10001);
        }
        if( !params.containsKey("password") ) {
            throw new ProcessException("password不能为空", 10001);
        }
        RSA rsa = new RSA();
        String before = rsa.decrypt(params.getString("password"), params.getString("appid"));

        try {
            decipher = JSONObject.parseObject(before);
        } catch (JSONException e) {
            throw new ProcessException("解密时出错：" + e.getMessage(), 10003);
        }

        // 校验请求中的时间戳是不是在服务器时间的前后 600 秒以内
        if(Config.getInstance().getEnableValidateTimestamp()) {
            if( !decipher.containsKey("timestamp") ) {
                throw new ProcessException("timestamp不能为空", 10001);
            }
            long current = System.currentTimeMillis() / 1000;
            long min = current - 300;
            long max = current + 300;
            long timestamp = decipher.getLong("timestamp");
            if( max < timestamp || min > timestamp ) {
                throw new ProcessException("请求已过期", 10004);
            }
        }
    }
}
