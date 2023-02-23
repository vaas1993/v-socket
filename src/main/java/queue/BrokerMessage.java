package queue;

import java.util.concurrent.ConcurrentLinkedQueue;

public class BrokerMessage {
    /**
     * 待处理消息列表
     * 每次添加的消息都在这里，有程序专门负责处理这个列表
     * 这个列表的键名 = 应用ID + "_" + 用户ID，键值 = 消息字符串数组
     */
    private ConcurrentLinkedQueue<String[]> MESSAGE_LIST = new ConcurrentLinkedQueue<>();

    private BrokerMessage() {
    }

    private final static BrokerMessage instance = new BrokerMessage();

    /**
     * 获取消息处理实例
     *
     * @return BrokerMessage
     */
    public static BrokerMessage getInstance() {
        return instance;
    }

    /**
     * 获取待处理消息的数量
     * @return int
     */
    public int getMessageListSize() {
        return MESSAGE_LIST.size();
    }

    /**
     * 添加一条新消息
     *
     * @param appid   String 应用ID
     * @param id      String 用户ID
     * @param message String 消息内容
     */
    public void push(String appid, String id, String message) {
        // 消息体是一个字符串数组，第一个元素是应用ID，第二个元素是用户ID，第三个元素是消息内容
        String[] item = {
                appid,
                id,
                message
        };
        MESSAGE_LIST.add(item);
    }

    /**
     * 拉取一条消息，当返回 null 时表示队列为空
     *
     * @return String[] | null
     */
    public String[] pull() {
        if (MESSAGE_LIST.isEmpty()) {
            return null;
        }
        return MESSAGE_LIST.poll();
    }
}
