package services.actions.sockets;

import com.alibaba.fastjson.JSONObject;

public class HeartbeatAction extends BaseSocketAction {
    @Override
    public JSONObject run() {
        return new JSONObject();
    }
}
