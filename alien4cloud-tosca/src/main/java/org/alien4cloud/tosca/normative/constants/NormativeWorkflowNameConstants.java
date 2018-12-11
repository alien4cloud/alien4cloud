package org.alien4cloud.tosca.normative.constants;

import com.google.common.collect.Sets;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Set;

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
    public static final String CANCEL = "cancel";

    public static final Set<String> STANDARD_WORKFLOWS = Sets.newHashSet(INSTALL, UNINSTALL, START, STOP, RUN, CANCEL);

    /** Not officially normative but static in alien4cloud */
    public static final String POST_UPDATE = "post_update";
}
