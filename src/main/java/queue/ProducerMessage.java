package queue;

/**
 * 消息生产者
 * 这里只是简单的把消息添加到队列中
 */
public class ProducerMessage {
    public String appid, id, message;
    /**
     * @param appid   String 应用ID
     * @param id      String 用户ID
     * @param message String 消息内容
     */
    public ProducerMessage(String appid, String id, String message) {
        this.appid = appid;
        this.id = id;
        this.message = message;
    }

    /**
     * 将消息添加到消息队列中
     */
    public void push() {
        BrokerMessage.getInstance().push(appid, id, message);
    }
}
