package alien4cloud.events;

import lombok.Getter;

@Getter
public class BeforeEnvironmentDeletedEvent extends AlienEvent {

    private String environmentId;

    public BeforeEnvironmentDeletedEvent(Object source, String environmentId) {
        super(source);
        this.environmentId = environmentId;
    }
}
