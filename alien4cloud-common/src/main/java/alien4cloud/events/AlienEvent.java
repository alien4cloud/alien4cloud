package alien4cloud.events;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

/**
 * Events that can be published through {@link ApplicationContext} and consumed by {@link ApplicationListener}s.
 * <p>
 * If published through main application context, they are broadcasted to child contexts (plugins) by ChildContextAspectsManager. When published by child
 * context components, they are naturally broadcasted to main context by spring.
 */
public abstract class AlienEvent extends ApplicationEvent {

    private static final long serialVersionUID = -2369666531188588829L;

    public AlienEvent(Object source) {
        super(source);
    }
    
}
