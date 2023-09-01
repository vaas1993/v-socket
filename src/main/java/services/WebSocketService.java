package services;

import channel.ChannelManager;
import exceptions.ProcessException;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.ssl.NotSslRecordException;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import services.actions.sockets.BaseSocketAction;
import utils.ActionHelper;
import utils.Config;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLHandshakeException;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

import static io.netty.handler.codec.http.HttpUtil.isKeepAlive;

public class WebSocketService extends BaseService {
    private static final Logger LOGGER = LogManager.getLogger(WebSocketService.class);

    @Override
    public void run() {
        int port = Config.getInstance().getSocketPort();

        // 定义两个事件组，一个用来接收请求，一个用来处理请求
        NioEventLoopGroup bosses = new NioEventLoopGroup();
        NioEventLoopGroup workers = new NioEventLoopGroup();
        // 初始化服务
        ServerBootstrap server = new ServerBootstrap();
        server.group(bosses, workers);
        server.channel(NioServerSocketChannel.class);

        SocketInitialization init = new SocketInitialization();
        server.childHandler(init);

        try {
            Channel channel = server.bind(port).sync().channel();
            LOGGER.info("WebSocket服务启动成功，端口号：" + port);
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage());
        } finally {
            bosses.shutdownGracefully();
            workers.shutdownGracefully();
            LOGGER.info("WebSocket服务已关闭");
        }

    }
}

class SocketInitialization extends ChannelInitializer<SocketChannel> {
    static final Logger LOGGER = LogManager.getLogger(SocketInitialization.class);

    @Override
    protected void initChannel(SocketChannel ch) {
        Config config = Config.getInstance();
        ChannelPipeline pp = ch.pipeline();

        pp.addLast("http-codec", new HttpServerCodec());
        pp.addLast("aggregator", new HttpObjectAggregator(65536));
        pp.addLast("http-chunked", new ChunkedWriteHandler());

        // 具体处理请求逻辑的是这个类
        pp.addLast("handler", new SocketHandler());

        // 静默超时断开连接
        pp.addLast(new IdleStateHandler(0, 0, config.getSocketTimeout(), TimeUnit.SECONDS));
        pp.addLast(new ExceptionHandler());

        // 开启wss支持
        if (config.getEnableSSL()) {
            SSLContext sslContext = SSLUtil.createSSLContext();
            if (sslContext == null) {
                LOGGER.error("SSL启动失败");
                ch.close();
                return;
            }
            SSLEngine ssl = sslContext.createSSLEngine();
            ssl.setNeedClientAuth(false);
            ssl.setUseClientMode(false);
            pp.addFirst("sslHandler", new SslHandler(ssl));
        }
    }
}

class SSLUtil {
    static final Logger LOGGER = LogManager.getLogger(SSLUtil.class);

    private static volatile SSLContext sslContext = null;

    public static SSLContext createSSLContext() {
        if (null == sslContext) {
            synchronized (SSLUtil.class) {
                if (null == sslContext) {
                    Config config = Config.getInstance();
                    String password = config.getSSLCertificatePassword();
                    String cert = config.getSSLCertificatePath();
                    String type = "PKCS12";
                    String path = System.getProperty("user.dir") + "/" + cert;
                    // 支持JKS、PKCS12（这里用的是阿里云免费申请的证书，下载tomcat解压后的pfx文件，对应PKCS12）
                    try {
                        KeyStore ks = KeyStore.getInstance(type);
                        // 证书存放地址
                        FileInputStream ksInputStream = new FileInputStream(path);
                        ks.load(ksInputStream, password.toCharArray());
                        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                        kmf.init(ks, password.toCharArray());
                        sslContext = SSLContext.getInstance("TLS");
                        sslContext.init(kmf.getKeyManagers(), null, null);
                    } catch (KeyStoreException | UnrecoverableKeyException | CertificateException | IOException |
                             NoSuchAlgorithmException | KeyManagementException e) {
                        LOGGER.error("SSL启动时出错：" + e.getMessage());
                    }
                }
            }
        }
        return sslContext;
    }
}

class ExceptionHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LogManager.getLogger(ExceptionHandler.class);

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Throwable exception = cause.getCause();

        if (exception instanceof NotSslRecordException) {
            LOGGER.error("服务仅支持 wss，客户端尝试使用 ws 连接");
            return;
        }
        if (exception instanceof SSLHandshakeException) {
            if (cause.getCause().getMessage().equals("Received fatal alert: certificate_unknown")) {
                LOGGER.error("SSL证书和访问的域名不匹配");
                return;
            }
        }
        if (exception == null) {
            LOGGER.error("发生未知的异常");
            return;
        }
        LOGGER.error(cause.getCause());
    }
}

class SocketHandler extends SimpleChannelInboundHandler<Object> {
    private WebSocketServerHandshaker shaker;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        // 浏览器发起 WebSocket 连接的时候，会先发起一个 HTTP 请求来创建连接，所以这里要处理两种请求类型
        if (msg instanceof FullHttpRequest) {
            onHTTP(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            JSONObject response = new JSONObject();
            try {
                response.putAll(onSocket(ctx, (WebSocketFrame) msg));
            } catch (ProcessException e) {
                response.put("code", e.getStatus());
                response.put("message", e.getMessage());
            }
            // 返回给客户端
            ctx.channel().writeAndFlush(new TextWebSocketFrame(response.toString()));
        }
    }

    /**
     * 处理 HTTP 请求，创建 WebSocket 连接
     */
    public void onHTTP(ChannelHandlerContext ctx, FullHttpRequest req) {
        //若不是 WebSocket 方式，则创建 BAD_REQUEST，返回给客户端
        if (!req.decoderResult().isSuccess() || (!"websocket".equals(req.headers().get("Upgrade")))) {
            DefaultFullHttpResponse res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
            // 返回应答给客户端
            if (res.status().code() != 200) {
                ByteBuf buf = Unpooled.copiedBuffer(res.status().toString(),
                        CharsetUtil.UTF_8);
                res.content().writeBytes(buf);
                buf.release();
            }
            ChannelFuture f = ctx.channel().writeAndFlush(res);
            // 如果是非 Keep-Alive，关闭连接
            if (!isKeepAlive(req) || res.status().code() != 200) {
                f.addListener(ChannelFutureListener.CLOSE);
            }
            return;
        }

        // 如果是正常的请求，则创建 WebSocket 连接
        WebSocketServerHandshakerFactory factory = new WebSocketServerHandshakerFactory(
                "",
                null,
                false
        );

        shaker = factory.newHandshaker(req);
        if (shaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
            return;
        }
        shaker.handshake(ctx.channel(), req);
    }

    /**
     * 处理 WebSocket 请求
     */
    public JSONObject onSocket(ChannelHandlerContext ctx, WebSocketFrame frame) throws ProcessException {
        JSONObject response = new JSONObject();
        response.put("code", 200);
        response.put("message", "操作成功");

        // 判断是断开连接的请求就断开
        if (frame instanceof CloseWebSocketFrame) {
            shaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            return response;
        }

        // 判断是否ping消息
        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(new PingWebSocketFrame(frame.content().retain()));
            return response;
        }

        // 其它非文本消息的请求，都返回错误
        if (!(frame instanceof TextWebSocketFrame)) {
            throw new ProcessException("不支持 " + frame.getClass().getName() + " 类型请求", 10000);
        }

        // 接收请求内容
        String req = ((TextWebSocketFrame) frame).text();
        JSONObject data;
        try {
            data = JSONObject.parseObject(req);
        } catch (JSONException e) {
            throw new ProcessException("请求的数据格式不是正确的 JSON", 10002);
        }

        if (!data.containsKey("action") || data.getString("action") == null) {
            throw new ProcessException("action 字段不能为空", 10001);
        }

        // 将 action 字段实例化成 services.actions.sockets 里对应的类
        String actionName = ActionHelper.route2name(data.getString("action"));
        actionName = "services.actions.sockets." + actionName;
        BaseSocketAction action = (BaseSocketAction) ActionHelper.name2instance(actionName);

        JSONObject params = new JSONObject();
        if (data.containsKey("data") && data.getJSONObject("data") != null) {
            params = data.getJSONObject("data");
        }

        action.setParams(params);
        action.setChannel(ctx.channel());
        action.beforeRun();
        JSONObject res = action.run();
        String[] fields = res.keySet().toArray(new String[0]);
        for (String field : fields) {
            response.put(field, res.get(field));
        }
        return response;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        // 客户端主动断开连接，需要将对应的 Channel 删除
        ChannelManager.getInstance().remove(ctx.channel());
    }
}