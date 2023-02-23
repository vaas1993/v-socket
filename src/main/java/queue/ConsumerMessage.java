package queue;

import channel.ChannelManager;
import com.alibaba.fastjson.JSONObject;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

/**
 * 消息消费者
 */
public class ConsumerMessage {
    public String appid, id, message;

    /**
     * @param appid   String 应用ID
     * @param id      String 用户ID
     * @param message String 消息内容
     */
    public ConsumerMessage(String appid, String id, String message) {
        this.appid = appid;
        this.id = id;
        this.message = message;
    }

    /**
     * 将消息发送到客户端
     */
    public void send() {
        ChannelManager cm = ChannelManager.getInstance();
        Channel channel = cm.get(appid, id);
        if( channel == null ) {
            // TODO 如果用户不在线则添加到离线消息，等下次上线再处理
            return;
        }
        JSONObject response = new JSONObject();
        response.put("message", message);
        response.put("type", "RECEIVED");
        channel.writeAndFlush(new TextWebSocketFrame(response.toString()));
    }
}
