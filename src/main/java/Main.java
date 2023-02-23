import queue.ParserMessage;
import services.HTTPService;
import services.WebSocketService;

public class Main {
    public static void main(String[] args) {
        // 打开HTTP服务
        HTTPService http = new HTTPService();
        http.start();

        // 打开WebSocket服务
        WebSocketService socket = new WebSocketService();
        socket.start();

        // 打开消息处理线程
        ParserMessage parser = new ParserMessage();
        parser.start();
    }
}