package services.actions;

import exceptions.ProcessException;
import com.alibaba.fastjson.JSONObject;

public abstract class BaseAction {
    public JSONObject params = new JSONObject();

    public abstract JSONObject run() throws ProcessException;

    public void setParams(JSONObject params) {
        this.params = params;
    }

    /**
     * 请求前的校验，抛出 ProcessException 异常后将不再执行 run 方法
     */
    public void beforeRun() throws ProcessException {
    }
}
