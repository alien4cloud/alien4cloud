package alien4cloud.events;

import lombok.Getter;

/**
 * This event may be triggered during leader election process in case of HA setup.
 */
@Getter
public class HALeaderElectionEvent extends AlienEvent {

    private static final long serialVersionUID = -1126617350064097857L;

    /**
     * Indicates if the current instance is elected as leader or banished.
     */
    private boolean leader;

    public HALeaderElectionEvent(Object source, boolean leader) {
        super(source);
        this.leader = leader;
    }

}
