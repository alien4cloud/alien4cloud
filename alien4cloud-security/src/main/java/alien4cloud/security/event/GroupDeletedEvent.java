package alien4cloud.security.event;

import alien4cloud.events.AlienEvent;
import alien4cloud.security.model.Group;
import lombok.Getter;

/**
 * Event triggered after a group is deleted
 */
@Getter
public class GroupDeletedEvent extends AlienEvent {
    private final Group group;

    public GroupDeletedEvent(Object source, Group group) {
        super(source);
        this.group = group;
    }
}
