package services.actions.https;

import exceptions.ProcessException;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import queue.ProducerMessage;
import services.actions.BaseAction;

/**
 * 消息批量推送接口处理
 */
public class MessageBatchAction extends BaseAction {
    private final static Logger LOGGER = LogManager.getLogger(MessageBatchAction.class);

    JSONArray raw;

    @Override
    public JSONObject run() {
        for (int i = 0; i < raw.size(); i++) {
            JSONObject item = raw.getJSONObject(i);
            ProducerMessage p = new ProducerMessage(
                    params.getString("APPID"),
                    item.getString("id"),
                    item.getString("message")
            );
            p.push();
        }

        JSONObject request = new JSONObject();
        request.put("message", "操作成功");
        return request;
    }

    @Override
    public void beforeRun() throws ProcessException {
        try {
            raw = JSONArray.parseArray(params.getString("RAW"));
            if (raw.size() == 0) {
                throw new ProcessException("请求数据不能为空", 10001);
            }
            for (int i = 0; i < raw.size(); i++) {
                JSONObject item = raw.getJSONObject(i);
                if (!item.containsKey("id") || !item.containsKey("message")) {
                    throw new ProcessException("第 " + i + " 项的 id 或 message 为空", 10002);
                }
            }
        } catch (JSONException | ClassCastException e) {
            LOGGER.error("批量推送消息时数据格式化出错：" + e.getMessage());
            throw new ProcessException("请求的数据格式不正确", 10002);
        }
    }
}
