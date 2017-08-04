package org.alien4cloud.server;

import java.util.Date;
import java.util.List;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Maintenance mode task is an object that stores state element on a maintenance mode task that blocks alien4cloud server from any user operations.
 */
@Getter
@Setter
@ESObject
public class MaintenanceModeState {
    public static final String MMS_ID = "mms_id";

    @Id
    private String id = MMS_ID;
    /** Flag to know if the maintenance mode was triggered by user or automatically triggered by internal process. */
    private boolean isUserTriggered;
    /** The last user that updated the maintenance mode. This is a pure informative element. */
    private String user;
    /** Optional message to display to users */
    private List<MaintenanceLog> log;
    /** Optional completion percentage of the maintenance. */
    private Integer progressPercent;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class MaintenanceLog {
        /** Log date. */
        private long date;
        /** User author of the log. */
        private String user;
        /** Message log */
        private String message;

        public MaintenanceLog(String user, String message) {
            this.date = new Date().getTime();
            this.user = user;
            this.message = message;
        }
    }
}