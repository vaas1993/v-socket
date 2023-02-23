package services.actions.sockets;

import io.netty.channel.Channel;
import services.actions.BaseAction;

public abstract class BaseSocketAction extends BaseAction {
    Channel channel;

    public void setChannel(Channel channel) {
        this.channel = channel;
    }
}
