package alien4cloud.it.utils.websocket;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

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

    private String host;

    private int port;

    private String user;

    private String password;

    private Map<String, String> headers;

    private String endPoint;

    private String loginPath;

    private String topic;

    private Channel stompChannel;

    private EventLoopGroup eventLoopGroup;

    /**
     * Create a stomp connection which perform login
     * 
     * @param host
     * @param port
     * @param user
     * @param password
     * @param endPoint
     * @param loginPath
     * @param topic
     */
    public StompConnection(String host, int port, String user, String password, String loginPath, String endPoint, String topic) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        this.loginPath = loginPath;
        this.endPoint = endPoint;
        this.topic = topic;
    }

    /**
     * Create a stomp connection
     * 
     * @param host
     * @param port
     * @param endPoint
     * @param topic
     */
    public StompConnection(String host, int port, String endPoint, String topic) {
        this(host, port, null, endPoint, topic);
    }

    /**
     * Create a stomp connection by adding to handshake request specified headers
     * 
     * @param host
     * @param port
     * @param headers
     * @param endPoint
     * @param topic
     */
    public StompConnection(String host, int port, Map<String, String> headers, String endPoint, String topic) {
        this.host = host;
        this.port = port;
        this.headers = headers;
        this.endPoint = endPoint;
        this.topic = topic;
    }

    /**
     * Start listening on web socket for specific data type. When data arrived the callback onData method will be called, when error happens the onError method
     * will be notified
     * 
     * @param dataType type of the data
     * @param callback the callback to notify data or error
     * @param <T> type of the data
     */
    @SneakyThrows({ InterruptedException.class, URISyntaxException.class })
    public <T> void listen(Class<T> dataType, IStompCallback<T> callback) {
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
        final StompClientHandler stompClientHandler = new StompClientHandler(topic, callback, dataType);
        DefaultHttpHeaders handshakeHeaders = new DefaultHttpHeaders();
        if (this.headers != null) {
            for (Map.Entry<String, String> header : this.headers.entrySet()) {
                handshakeHeaders.add(header.getKey(), header.getValue());
            }
        }
        final WebSocketClientHandler webSocketHandler = new WebSocketClientHandler(
                WebSocketClientHandshakerFactory.newHandshaker(
                        new URI(wsUrl),
                        WebSocketVersion.V13,
                        null,
                        false,
                        handshakeHeaders
                        ),
                host,
                user,
                password,
                loginUrl,
                callback
                );
        Bootstrap b = new Bootstrap();
        b.group(eventLoopGroup).channel(NioSocketChannel.class);
        b.handler(new ChannelInitializer<SocketChannel>() {

            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(HttpClientCodec.class.getName(), new HttpClientCodec());
                pipeline.addLast(HttpObjectAggregator.class.getName(), new HttpObjectAggregator(8192));
                pipeline.addLast(WebSocketClientCompressionHandler.class.getName(), new WebSocketClientCompressionHandler());
                pipeline.addLast(WebSocketClientHandler.class.getName(), webSocketHandler);
                pipeline.addLast(StompSubframeDecoder.class.getName(), new StompSubframeDecoder());
                pipeline.addLast(StompSubframeEncoder.class.getName(), new StompSubframeEncoder());
                pipeline.addLast(StompSubframeAggregator.class.getName(), new StompSubframeAggregator(1048576));
                pipeline.addLast(StompClientHandler.class.getName(), stompClientHandler);
            }
        });
        this.stompChannel = b.connect(host, port).sync().channel();
        webSocketHandler.handshakeFuture().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(final ChannelFuture future) throws Exception {
                stompClientHandler.beginStomp(stompChannel);
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
     * @return the future retrieved data
     */
    public IStompDataFuture<String> getData() {
        StompCallback<String> callback = new StompCallback<>();
        listen(String.class, callback);
        return callback;
    }

    /**
     * Try to retrieve a given amount of the given type of data.
     * This method is asynchronous, it returns immediately
     *
     * @param dataType type of data to retrieve
     * @return the future retrieved data
     */
    public <T> IStompDataFuture<T> getData(Class<T> dataType) {
        StompCallback<T> callback = new StompCallback<>();
        listen(dataType, callback);
        return callback;
    }
}
