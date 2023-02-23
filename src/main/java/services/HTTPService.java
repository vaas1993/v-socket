package services;

import exceptions.ProcessException;
import com.alibaba.fastjson.JSONObject;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import services.actions.BaseAction;
import utils.ActionHelper;
import utils.Config;
import utils.Database;
import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpUtil.is100ContinueExpected;

public class HTTPService extends BaseService {
    private final static Logger LOGGER = LogManager.getLogger(HTTPService.class);

    @Override
    public void run() {
        // 定义两个事件组，一个用来接收请求，一个用来处理请求
        NioEventLoopGroup bosses = new NioEventLoopGroup();
        NioEventLoopGroup workers = new NioEventLoopGroup();

        // 初始化服务
        ServerBootstrap server = new ServerBootstrap();
        server.group(bosses, workers);
        server.channel(NioServerSocketChannel.class);
        server.childHandler(new HTTPServerInitialization());

        int port = Config.getInstance().getHTTPPort();
        try {
            Channel channel = server.bind(port).sync().channel();
            LOGGER.info("HTTP服务启动成功，端口号：" + port);
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage());
        } finally {
            bosses.shutdownGracefully();
            workers.shutdownGracefully();
            LOGGER.info("HTTP服务已关闭");
        }
    }
}

class HTTPServerInitialization extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) {
        ch.pipeline().addLast(new HttpServerCodec());
        ch.pipeline().addLast("httpAggregator", new HttpObjectAggregator(512 * 1024));
        ch.pipeline().addLast(new HTTPServerHandler());// 请求处理器
    }
}

/**
 * HTTP请求的处理器
 */
class HTTPServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) {
        //100 Continue
        if (is100ContinueExpected(req)) {
            ctx.write(new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.CONTINUE));
        }

        // 响应的数据
        JSONObject responseData = new JSONObject();
        responseData.put("code", 200);
        responseData.put("message", "成功");

        try {
            // 请求参数，这里会分为 GET 和 POST 两个元素
            JSONObject params = new JSONObject();
            params.put("GET", this.getGetParams(req));
            params.put("RAW", req.content().toString(CharsetUtil.UTF_8));
            params.put("APPID", validate(req));

            // 将路由名转成操作类名
            String actionName = ActionHelper.route2name(req.uri());
            actionName = "services.actions.https." + actionName;
            BaseAction action = (BaseAction)ActionHelper.name2instance(actionName);

            // 将请求参数传递给接口处理
            action.setParams(params);
            action.beforeRun();
            // 接口返回的数据直接根 responseData 合并
            responseData.putAll(action.run());
        } catch (ProcessException e) {
            responseData.put("code", e.getStatus());
            responseData.put("message", e.getMessage());
        }

        // 创建response
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.copiedBuffer(responseData.toString(), CharsetUtil.UTF_8));
        // 设置头信息
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        // 将response返回到客户端
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * 获取 GET 参数
     */
    public JSONObject getGetParams(FullHttpRequest req) {
        String uri = req.uri();
        QueryStringDecoder decoder = new QueryStringDecoder(uri);
        Map<String, List<String>> parameters = decoder.parameters();
        JSONObject params = new JSONObject();
        for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
            List<String> value = entry.getValue();
            params.put(entry.getKey(), value.size() == 1 ? value.get(0) : value);
        }
        return params;
    }

    /**
     * 请求校验，校验通过时返回请求对应的应用ID
     *
     * @param req FullHttpRequest 请求数据
     * @return String
     */
    public String validate(FullHttpRequest req) throws ProcessException {
        String token = req.headers().get("Authorization");
        if (token != null) {
            if (token.indexOf("Bearer ") == 0) {
                token = token.replaceAll("Bearer ", "");

                // token 即 应用ID，这里直接查询应用ID是否在数据库中存在
                try {
                    Database.getInstance().getCertificate(token);
                } catch (ProcessException e) {
                    throw new ProcessException(e.getMessage(), e.getStatus());
                }
                return token;
            }
        }
        throw new ProcessException("未授权访问", 401);
    }
}