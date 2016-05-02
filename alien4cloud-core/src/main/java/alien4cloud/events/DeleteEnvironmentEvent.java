package alien4cloud.events;

import lombok.Getter;

import org.springframework.context.ApplicationEvent;

import alien4cloud.model.application.ApplicationEnvironment;

@Getter
public class DeleteEnvironmentEvent extends ApplicationEvent {

    private static final long serialVersionUID = -1126617350064097857L;

    private ApplicationEnvironment applicationEnvironment;

    public DeleteEnvironmentEvent(Object source, ApplicationEnvironment applicationEnvironment) {
        super(source);
        this.applicationEnvironment = applicationEnvironment;
    }

}
