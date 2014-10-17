package alien4cloud.rest.websocket;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptorAdapter;
import org.springframework.stereotype.Component;

@Slf4j
@Component("webSocketTopicSubscriptionInterceptor")
public class WebSocketTopicSubscriptionInterceptor extends ChannelInterceptorAdapter {

    @Resource
    private ISecuredHandler[] handlers;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(message);
        if (StompCommand.SUBSCRIBE.equals(headerAccessor.getCommand())) {
            String destination = headerAccessor.getDestination();
            for (ISecuredHandler handler : handlers) {
                if (handler.canHandleDestination(destination)) {
                    handler.checkAuthorization(headerAccessor.getUser(), destination);
                }
            }
        }
        return message;
    }
}
