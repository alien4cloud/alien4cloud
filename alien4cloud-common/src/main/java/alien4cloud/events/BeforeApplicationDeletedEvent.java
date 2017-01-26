package alien4cloud.events;

import lombok.Getter;

@Getter
public class BeforeApplicationDeletedEvent extends AlienEvent {

    private String applicationId;

    public BeforeApplicationDeletedEvent(Object source, String applicationId) {
        super(source);
        this.applicationId = applicationId;
    }
}
