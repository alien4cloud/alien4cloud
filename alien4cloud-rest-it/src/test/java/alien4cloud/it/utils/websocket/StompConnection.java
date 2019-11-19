package alien4cloud.it.utils.websocket;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.codec.stomp.DefaultStompFrame;
import io.netty.handler.codec.stomp.StompCommand;
import io.netty.handler.codec.stomp.StompFrame;
import io.netty.handler.codec.stomp.StompHeaders;
import io.netty.handler.codec.stomp.StompSubframeAggregator;
import io.netty.handler.codec.stomp.StompSubframeDecoder;
import io.netty.handler.codec.stomp.StompSubframeEncoder;

/**
 * Java implementation for stomp over web socket
 * 
 * @author Minh Khang VU
 */
@Slf4j
public class StompConnection {

    private static AtomicInteger COUNTER = new AtomicInteger(0);

    private String host;

    private int port;

    private String user;

    private String password;

    private Map<String, String> headers;

    private String endPoint;

    private String loginPath;

    private Channel stompChannel;

    private EventLoopGroup eventLoopGroup;

    private StompClientHandler stompClientHandler;

    /**
     * Create a stomp connection which perform login
     * 
     * @param host
     * @param port
     * @param user
     * @param password
     * @param endPoint
     * @param loginPath
     */
    public StompConnection(String host, int port, String user, String password, String loginPath, String endPoint) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        this.loginPath = loginPath;
        this.endPoint = endPoint;
        init();
    }

    /**
     * Create a stomp connection
     * 
     * @param host
     * @param port
     * @param endPoint
     */
    public StompConnection(String host, int port, String endPoint) {
        this(host, port, null, endPoint);
    }

    /**
     * Create a stomp connection by adding to handshake request specified headers
     * 
     * @param host
     * @param port
     * @param headers
     * @param endPoint
     */
    public StompConnection(String host, int port, Map<String, String> headers, String endPoint) {
        this.host = host;
        this.port = port;
        this.headers = headers;
        this.endPoint = endPoint;
        init();
    }

    @SneakyThrows({ InterruptedException.class, URISyntaxException.class })
    private void init() {
        if (this.stompChannel != null) {
            throw new IllegalStateException("The stomp connection has already been started");
        }
        String wsUrl = "ws://" + host + ":" + port + endPoint + "/websocket";
        if (log.isDebugEnabled()) {
            log.debug("Web socket url {}", wsUrl);
        }
        String loginUrl = null;
        if (user != null && password != null && loginPath != null) {
            loginUrl = "http://" + host + ":" + port + loginPath;
            if (log.isDebugEnabled()) {
                log.debug("Authentication url {}", loginUrl);
            }
        }
        this.eventLoopGroup = new NioEventLoopGroup();
        this.stompClientHandler = new StompClientHandler();
        DefaultHttpHeaders handshakeHeaders = new DefaultHttpHeaders();
        if (this.headers != null) {
            for (Map.Entry<String, String> header : this.headers.entrySet()) {
                handshakeHeaders.add(header.getKey(), header.getValue());
            }
        }
        final WebSocketClientHandler webSocketHandler = new WebSocketClientHandler(WebSocketClientHandshakerFactory.newHandshaker(new URI(wsUrl),
                WebSocketVersion.V13, null, false, handshakeHeaders), host, user, password, loginUrl);
        WebSocketClientOutHandler webSocketOutHandler = new WebSocketClientOutHandler(30);
        Bootstrap b = new Bootstrap();
        b.group(eventLoopGroup).channel(NioSocketChannel.class);
        b.handler(new ChannelInitializer<SocketChannel>() {

            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(HttpClientCodec.class.getName(), new HttpClientCodec());
                pipeline.addLast(HttpObjectAggregator.class.getName(), new HttpObjectAggregator(8192));
                //pipeline.addLast(WebSocketClientCompressionHandler.class.getName(), new WebSocketClientCompressionHandler());
                pipeline.addLast(WebSocketClientCompressionHandler.class.getName(), WebSocketClientCompressionHandler.INSTANCE);
                pipeline.addLast(WebSocketClientHandler.class.getName(), webSocketHandler);
                pipeline.addLast(WebSocketClientOutHandler.class.getName(), webSocketOutHandler);
                pipeline.addLast(StompSubframeDecoder.class.getName(), new StompSubframeDecoder());
                pipeline.addLast(StompSubframeEncoder.class.getName(), new StompSubframeEncoder());
                pipeline.addLast(StompSubframeAggregator.class.getName(), new StompSubframeAggregator(1048576));
                pipeline.addLast(StompClientHandler.class.getName(), stompClientHandler);
            }
        });
        this.stompChannel = b.connect(host, port).sync().channel();
        this.stompClientHandler.connectFuture(this.stompChannel.newPromise());
        webSocketHandler.handshakeFuture().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(final ChannelFuture future) throws Exception {
                stompClientHandler.beginStomp(stompChannel);
            }
        });
    }

    /**
     * Start listening on web socket for specific data type. When data arrived the callback onData method will be called, when error happens the onError method
     * will be notified
     * 
     * @param topic the topic to listen to
     * @param callback the callback to notify data or error
     * @param <T> type of the data
     */
    public <T> void listen(final String topic, IStompCallback<T> callback) {
        this.stompClientHandler.listen(topic, callback);
        this.stompClientHandler.connectFuture().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(final ChannelFuture future) throws Exception {
                StompFrame subscribeFrame = new DefaultStompFrame(StompCommand.SUBSCRIBE);
                subscribeFrame.headers().set(StompHeaders.DESTINATION, topic);
                subscribeFrame.headers().set(StompHeaders.ID, String.valueOf(COUNTER.incrementAndGet()));
                stompChannel.writeAndFlush(subscribeFrame);
            }
        });
    }

    /**
     * Close the stomp connection
     */
    public void close() {
        if (this.stompChannel == null) {
            throw new IllegalStateException("The stomp connection has not yet been started");
        }
        this.stompChannel.close();
        this.eventLoopGroup.shutdownGracefully();
    }

    /**
     * Try to retrieve a given amount of the given type of data.
     * This method is asynchronous, it returns immediately
     *
     * @param topic the topic to listen to
     * @return the future retrieved data
     */
    public IStompDataFuture<String> getData(String topic) {
        StompCallback<String> callback = new StompCallback<>(String.class);
        listen(topic, callback);
        return callback;
    }

    /**
     * Try to retrieve a given amount of the given type of data.
     * This method is asynchronous, it returns immediately
     * 
     * @param topic the topic to listen to
     * @param dataType type of data to retrieve
     * @return the future retrieved data
     */
    public <T> IStompDataFuture<T> getData(String topic, Class<T> dataType) {
        StompCallback<T> callback = new StompCallback<>(dataType);
        listen(topic, callback);
        return callback;
    }
}
