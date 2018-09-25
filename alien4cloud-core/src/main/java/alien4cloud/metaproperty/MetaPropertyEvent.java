package alien4cloud.metaproperty;

import org.springframework.context.ApplicationEvent;

public class MetaPropertyEvent extends ApplicationEvent {

    public MetaPropertyEvent(Object source) {
        super(source);
    }
}
