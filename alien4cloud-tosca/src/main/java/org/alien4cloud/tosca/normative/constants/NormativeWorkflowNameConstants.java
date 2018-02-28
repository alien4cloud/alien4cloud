package org.alien4cloud.tosca.normative.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Workflow names constants as acknowledge in TOSCA.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NormativeWorkflowNameConstants {
    public static final String INSTALL = "install";
    public static final String UNINSTALL = "uninstall";
    public static final String START = "start";
    public static final String STOP = "stop";
    public static final String RUN = "run";

    /** Not officially normative but static in alien4cloud */
    public static final String POST_UPDATE = "post_update";
}
