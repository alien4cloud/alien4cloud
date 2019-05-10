package alien4cloud.it.utils.websocket;

import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;
import alien4cloud.rest.utils.JsonUtil;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.stomp.DefaultStompFrame;
import io.netty.handler.codec.stomp.StompCommand;
import io.netty.handler.codec.stomp.StompFrame;
import io.netty.handler.codec.stomp.StompHeaders;

/**
 * @author Minh Khang VU
 */
@Slf4j
public class StompClientHandler extends SimpleChannelInboundHandler<Object> {

    private Map<String, IStompCallback> handlers = new ConcurrentHashMap<>();

    private ChannelPromise connectFuture;

    public void beginStomp(Channel channel) throws Exception {
        StompFrame connFrame = new DefaultStompFrame(StompCommand.CONNECT);
        connFrame.headers().set(StompHeaders.ACCEPT_VERSION, "1.2");
        channel.writeAndFlush(connFrame);
        if (log.isDebugEnabled()) {
            log.debug("Begin web socket connection");
        }
    }

    public ChannelPromise connectFuture() {
        return connectFuture;
    }

    public void connectFuture(ChannelPromise connectFuture) {
        this.connectFuture = connectFuture;
    }

    public void listen(String topic, IStompCallback callback) {
        this.handlers.put(topic, callback);
    }

    @Override
    //public void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception {
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        StompFrame frame = (StompFrame) msg;
        String destination = null;
        if (frame.headers().get(StompHeaders.DESTINATION) != null) {
            destination = frame.headers().get(StompHeaders.DESTINATION).toString();
        }
        if (log.isDebugEnabled()) {
            log.debug("Received frame {} from topic {}", toString(frame), destination);
        }
        IStompCallback callback = null;
        if (destination != null) {
            callback = handlers.get(destination);
            if (callback == null) {
                throw new IllegalStateException("Received message for a topic that was never registered before");
            }
        }
        switch (frame.command()) {
        case CONNECTED:
            connectFuture.setSuccess();
            break;
        case MESSAGE:
            if (String.class == callback.getExpectedDataType()) {
                callback.onData(frame.headers().get(StompHeaders.DESTINATION).toString(), frame.content().toString(Charset.forName("UTF-8")));
            } else {
                callback.onData(frame.headers().get(StompHeaders.DESTINATION).toString(),
                        JsonUtil.readObject(new ByteBufInputStream(frame.content()), callback.getExpectedDataType()));
            }
            break;
        case ERROR:
            String frameText = toString(frame);
            log.error("Received stomp error {} for topic {}", frameText, destination);
            callback.onError(new StompErrorException("Stomp error for destination " + destination + " :\n" + frameText));
            break;
        default:
            frameText = toString(frame);
            log.error("Received unknown frame {} for topic {}", frameText, destination);
            callback.onError(new StompUnknownCommandException("Unknown stomp command " + frame.command() + " from frame :\n" + frameText));
            break;
        }
    }

    private String toString(StompFrame frame) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("------------------------------------------------\n");
        buffer.append("COMMAND :").append(frame.command()).append("\n");
        buffer.append("------------------------------------------------\n");
        buffer.append("HEADERS :\n");
        Iterator<Entry<CharSequence, CharSequence>> headerIterator = frame.headers().iterator();
        while (headerIterator.hasNext()) {
            Entry<CharSequence, CharSequence> header = headerIterator.next();
            buffer.append(header.getKey()).append(" : ").append(header.getValue()).append("\n");
        }
        buffer.append("------------------------------------------------\n");
        buffer.append("CONTENT :\n");
        buffer.append("------------------------------------------------\n");
        buffer.append(frame.content().toString(Charset.forName("UTF-8"))).append("\n");
        return buffer.toString();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Stomp error", cause);
        ctx.close();
        for (IStompCallback callback : handlers.values()) {
            callback.onError(cause);
        }
    }
}
