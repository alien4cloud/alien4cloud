package alien4cloud.events;

import org.springframework.context.ApplicationEvent;

public abstract class AlienEvent extends ApplicationEvent {

    public AlienEvent(Object source) {
        super(source);
    }
    
}
