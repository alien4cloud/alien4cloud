package alien4cloud.it.utils.websocket;

import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import alien4cloud.rest.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.stomp.DefaultStompFrame;
import io.netty.handler.codec.stomp.StompCommand;
import io.netty.handler.codec.stomp.StompFrame;
import io.netty.handler.codec.stomp.StompHeaders;

/**
 * @author Minh Khang VU
 */
@Slf4j
public class StompClientHandler<T> extends SimpleChannelInboundHandler<Object> {

    private static AtomicInteger counter;

    private String topic;

    private IStompCallback<T> callback;

    private Class<T> dataType;

    public StompClientHandler(String topic, IStompCallback<T> callback, Class<T> dataType) {
        this.topic = topic;
        this.callback = callback;
        this.dataType = dataType;
        this.counter = new AtomicInteger();
    }

    public void beginStomp(Channel channel) throws Exception {
        StompFrame connFrame = new DefaultStompFrame(StompCommand.CONNECT);
        connFrame.headers().set(StompHeaders.ACCEPT_VERSION, "1.2");
        channel.writeAndFlush(connFrame);
        if (log.isDebugEnabled()) {
            log.debug("Begin web socket connection");
        }
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception {

        StompFrame frame = (StompFrame) msg;
        switch (frame.command()) {
        case CONNECTED:
            if (log.isDebugEnabled()) {
                log.debug("Connected, sending subscribe for topic ", this.topic);
            }
            StompFrame subscribeFrame = new DefaultStompFrame(StompCommand.SUBSCRIBE);
            subscribeFrame.headers().set(StompHeaders.DESTINATION, topic);
            subscribeFrame.headers().set(StompHeaders.ID, String.valueOf(this.counter.incrementAndGet()));
            ctx.writeAndFlush(subscribeFrame);
            break;
        case MESSAGE:
            if (log.isDebugEnabled()) {
                log.debug("Received frame {} from topic {}", toString(frame), this.topic);
            }
            if (String.class == this.dataType) {
                this.callback.onData(frame.headers().get(StompHeaders.DESTINATION).toString(), (T) frame.content().toString(Charset.forName("UTF-8")));
            } else {
                this.callback
                        .onData(frame.headers().get(StompHeaders.DESTINATION).toString(), JsonUtil.readObject(new ByteBufInputStream(frame.content()), this.dataType));
            }
            break;
        case ERROR:
            String frameText = toString(frame);
            log.error("Received stomp error {}", frameText);
            this.callback.onError(new StompErrorException("Stomp error :\n" + frameText));
            break;
        default:
            frameText = toString(frame);
            log.error("Received unknown frame {}", frameText);
            this.callback.onError(new StompUnknownCommandException("Unknown stomp command " + frame.command() + " from frame :\n" + frameText));
            break;
        }
    }

    private String toString(StompFrame frame) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("------------------------------------------------\n");
        buffer.append("COMMAND :").append(frame.command()).append("\n");
        buffer.append("------------------------------------------------\n");
        buffer.append("HEADERS :\n");
        Iterator<Map.Entry<CharSequence, CharSequence>> headerIterator = frame.headers().iterator();
        while (headerIterator.hasNext()) {
            Map.Entry<CharSequence, CharSequence> header = headerIterator.next();
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
        this.callback.onError(cause);
    }
}
