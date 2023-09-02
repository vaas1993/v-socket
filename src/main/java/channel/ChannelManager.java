package channel;

import io.netty.channel.Channel;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 连接管理器
 * 存储通过 WebSocket 过来的连接实例，并且提供一些方法让调用者可以方便的根据连接ID或者应用ID+用户ID来获取连接
 */
public class ChannelManager {
    /**
     * 连接列表，键名是连接ID，键值是连接实例
     */
    ConcurrentMap<String, Channel> CHANNEL_LIST = new ConcurrentHashMap<>();

    /**
     * 保存一份使用 应用ID和用户ID 作为键名，与之对应的连接ID
     * 在下发消息时，只能取到应用ID和用户ID，此时就需要查询出对应的连接ID
     */
    ConcurrentMap<String, String> APPID_ID_TO_CHANNEL_ID_LIST = new ConcurrentHashMap<>();

    /**
     * 保存一份使用 连接ID作为键名，与之对应的应用ID和用户ID
     */
    ConcurrentMap<String, String> CHANNEL_ID_TO_APPID_ID_LIST = new ConcurrentHashMap<>();

    private ChannelManager() {}

    private static final ChannelManager instance = new ChannelManager();

    public static ChannelManager getInstance() {
        return instance;
    }

    /**
     * 添加一个连接实例
     *
     * @param appid   String 应用ID
     * @param id      String 用户ID
     * @param channel Channel 连接实例
     */
    public void add(String appid, String id, Channel channel) {
        String key = id2key(appid, id);
        String channelID = channel.id().asShortText();
        remove(appid, id);
        CHANNEL_LIST.put(channelID, channel);
        APPID_ID_TO_CHANNEL_ID_LIST.put(key, channelID);
        CHANNEL_ID_TO_APPID_ID_LIST.put(channelID, key);
    }

    /**
     * 删除连接
     * @param appid   String 应用ID
     * @param id      String 用户ID
     */
    public void remove(String appid, String id) {
        String key = id2key(appid, id);
        String channelID = APPID_ID_TO_CHANNEL_ID_LIST.get(key);
        APPID_ID_TO_CHANNEL_ID_LIST.remove(key);
        if( channelID != null ) {
            remove(channelID);
        }
    }

    /**
     * 删除连接
     * @param channel Channel 连接实例
     */
    public void remove(Channel channel) {
        String channelID = channel.id().asShortText();
        String key = CHANNEL_ID_TO_APPID_ID_LIST.get(channelID);
        if( key != null ) {
            APPID_ID_TO_CHANNEL_ID_LIST.remove(key);
        }
        remove(channelID);
    }

    /**
     * 删除并断开连接
     * @param channelID String 通道ID
     */
    public void remove(String channelID) {
        CHANNEL_ID_TO_APPID_ID_LIST.remove(channelID);
        Channel channel = CHANNEL_LIST.get(channelID);
        if( channel != null ) {
            channel.close();
        }
        CHANNEL_LIST.remove(channelID);
    }

    /**
     * 获取连接实例
     * @param appid   String 应用ID
     * @param id      String 用户ID
     */
    public Channel get(String appid, String id) {
        String key = id2key(appid, id);
        String channelId = APPID_ID_TO_CHANNEL_ID_LIST.get(key);
        Channel channel = null;
        if( channelId != null ) {
            channel = CHANNEL_LIST.get(channelId);
        }
        return channel;
    }

    /**
     * 应用ID和用户ID，返回一个唯一键名
     *
     * @param appid String 应用ID
     * @param id    String 用户ID
     */
    public String id2key(String appid, String id) {
        return appid + "_" + id;
    }
}
