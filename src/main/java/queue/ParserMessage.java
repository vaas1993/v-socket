package queue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utils.Config;

/**
 * 消息队列处理器
 * 如果自行实现使用了其它队列中间件，需要在 Main 类中停用它
 */
public class ParserMessage extends Thread {
    private final static Logger LOGGER = LogManager.getLogger(ParserMessage.class);
    synchronized public void run() {
        int interval = Config.getInstance().getQueueInterval();
        BrokerMessage broker = BrokerMessage.getInstance();

        LOGGER.info("消息队列处理器启动成功");

        while (true) {
            int size = broker.getMessageListSize();
            if( size == 0 ) {
                try {
                    wait(interval);
                } catch (InterruptedException e) {
                    LOGGER.error("暂停消息时出错：" + e.getMessage());
                }
                continue;
            }

            // 取出消息交给消费者
            for(int i = 0; i < size; i++) {
                String[] item = broker.pull();
                if( item == null ) {
                    continue;
                }
                ConsumerMessage c = new ConsumerMessage(item[0], item[1], item[2]);
                c.send();
            }
        }
    }
}