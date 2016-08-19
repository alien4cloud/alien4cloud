package alien4cloud.rest.websocket;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.broker.BrokerAvailabilityEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.*;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by lucboutier on 18/08/2016.
 */
@Slf4j
@Component
public class WebSocketLogger implements ApplicationListener {
    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof SessionConnectEvent || applicationEvent instanceof SessionConnectedEvent
                || applicationEvent instanceof SessionSubscribeEvent || applicationEvent instanceof SessionUnsubscribeEvent
                || applicationEvent instanceof SessionDisconnectEvent || applicationEvent instanceof BrokerAvailabilityEvent) {
            log.info(applicationEvent.getClass().getSimpleName() + " " + applicationEvent.toString());
        }
    }
}
