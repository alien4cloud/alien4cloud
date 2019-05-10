package alien4cloud.it.utils.websocket;

import java.nio.charset.Charset;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import alien4cloud.it.exception.ITException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
//import io.netty.handler.codec.AsciiString;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;

@Slf4j
public class WebSocketClientHandler<T> extends SimpleChannelInboundHandler<Object> {

    private final WebSocketClientHandshaker handShaker;

    private ChannelPromise handshakeFuture;

    private String host;

    private String user;

    private String password;

    private String authenticationUrl;

    private Set<Cookie> cookies;

    private final AttributeKey<Set<Cookie>> ctxCookies = AttributeKey.valueOf("cookies");

    public WebSocketClientHandler(WebSocketClientHandshaker handShaker, String host, String user, String password, String authenticationUrl) {
        this.handShaker = handShaker;
        this.host = host;
        this.user = user;
        this.password = password;
        this.authenticationUrl = authenticationUrl;
    }

    public ChannelFuture handshakeFuture() {
        return handshakeFuture;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        handshakeFuture = ctx.newPromise();
    }

/******************
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof HttpRequest) {
            if (this.cookies != null && !this.cookies.isEmpty()) {
                HttpRequest request = (HttpRequest) msg;
                //request.headers().set(new AsciiString("cookie"), ClientCookieEncoder.encode(cookies));
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
************************/

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (this.authenticationUrl != null) {
            HttpRequest loginRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, this.authenticationUrl);
            //loginRequest.headers().set(new AsciiString("host"), this.host);
            loginRequest.headers().set("host", this.host);
            HttpPostRequestEncoder bodyRequestEncoder = new HttpPostRequestEncoder(loginRequest, false);
            bodyRequestEncoder.addBodyAttribute("j_username", user);
            bodyRequestEncoder.addBodyAttribute("j_password", password);
            bodyRequestEncoder.addBodyAttribute("submit", "Login");
            loginRequest = bodyRequestEncoder.finalizeRequest();
            if (log.isDebugEnabled()) {
                log.debug("Authentication request for user {} to {}", this.user, this.authenticationUrl);
            }
            ctx.writeAndFlush(loginRequest);
        } else {
            handShaker.handshake(ctx.channel());
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (log.isDebugEnabled()) {
            log.debug("Channel disconnected");
        }
    }

    @Override
    //public void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception {
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        // The first message must be authentication response
        if (this.authenticationUrl != null && (this.cookies == null || this.cookies.isEmpty())) {
            HttpResponse response = (HttpResponse) msg;
            //CharSequence cookieData = response.headers().get(AsciiString("set-cookie"));
            CharSequence cookieData = response.headers().get("set-cookie");
            if (cookieData != null) {
                //this.cookies = ServerCookieDecoder.decode(cookieData.toString());
                this.cookies = CookieDecoder.decode(cookieData.toString());
                ctx.attr(ctxCookies).set(this.cookies);
                if (this.cookies == null || this.cookies.isEmpty()) {
                    throw new WebSocketAuthenticationFailureException("Could not authenticate");
                }
                if (log.isDebugEnabled()) {
                    for (Cookie cookie : this.cookies) {
                        log.debug("Server says must set cookie with name {} and value {}", cookie.name(), cookie.value());
                    }
                }
            } else {
                throw new ITException("Could not authenticate");
            }
            if (log.isDebugEnabled()) {
                log.debug("Authentication succeeded for user {}", this.user);
            }
            handShaker.handshake(ctx.channel());
            return;
        }

        // The second one must be the response for web socket handshake
        if (!handShaker.isHandshakeComplete()) {
            handShaker.finishHandshake(ctx.channel(), (FullHttpResponse) msg);
            if (log.isDebugEnabled()) {
                log.debug("Web socket client connected for user {}", this.user);
            }
            handshakeFuture.setSuccess();
            return;
        }
        // Take the byte buff and send it up to Stomp decoder
        if (msg instanceof WebSocketFrame) {
            if (log.isDebugEnabled()) {
                if (msg instanceof TextWebSocketFrame) {
                    log.debug("Received text frame {}", ((TextWebSocketFrame) msg).text());
                }
            }
            ReferenceCountUtil.retain(msg);
            ctx.fireChannelRead(((WebSocketFrame) msg).content());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Web socket error", cause);
        if (!handshakeFuture.isDone()) {
            handshakeFuture.setFailure(cause);
        }
        ctx.close();
    }
}
