package alien4cloud.security.event;

import alien4cloud.events.AlienEvent;
import alien4cloud.security.model.User;
import lombok.Getter;

/**
 * Event triggered after a user is deleted
 */
@Getter
public class UserDeletedEvent extends AlienEvent {
    private final User user;

    public UserDeletedEvent(Object source, User user) {
        super(source);
        this.user = user;
    }
}
