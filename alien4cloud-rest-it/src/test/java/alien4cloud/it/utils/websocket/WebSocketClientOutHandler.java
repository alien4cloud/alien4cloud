package alien4cloud.it.utils.websocket;

import java.nio.charset.Charset;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;

@Slf4j
public class WebSocketClientOutHandler extends WriteTimeoutHandler {

    public WebSocketClientOutHandler (int timeout) {
      super(timeout);
    }

    private final AttributeKey<Set<Cookie>> ctxCookies = AttributeKey.valueOf("cookies");

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof HttpRequest) {
            Set<Cookie> cookies = ctx.attr(ctxCookies).get();
            if (cookies != null && !cookies.isEmpty()) {
                HttpRequest request = (HttpRequest) msg;
                request.headers().set("cookie", ClientCookieEncoder.encode(cookies));
                if (log.isDebugEnabled()) {
                    log.debug("Write HttpRequest {} enriched with security cookie", request);
                }
            }
            super.write(ctx, msg, promise);
            return;
        }
        String wrappedFrame = ((ByteBuf) msg).toString(Charset.forName("UTF-8"));
        if (log.isDebugEnabled()) {
            log.debug("Write text frame {}", wrappedFrame);
        }
        WebSocketFrame webSocketFrame = new TextWebSocketFrame(wrappedFrame);
        super.write(ctx, webSocketFrame, promise);
    }

}
