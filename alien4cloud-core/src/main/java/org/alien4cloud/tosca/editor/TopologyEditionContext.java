package org.alien4cloud.tosca.editor;

import alien4cloud.tosca.context.ToscaContext;

/**
 * Topology edition context is related to a specific topology that is currently under edition.
 * 
 * Edition context is closed automatically when no users are currently editing it or when inactive after 5 minutes.
 */
public class TopologyEditionContext {
    /** The tosca context associated with the topology context. */
    private ToscaContext toscaContext;

    // Join
    public void join() {
        // register the user for activity monitor (to auto-leave)
    }

    // Leave edition context
    public void leave() {

    }

    public void close() {
        // close and destroy the topology context

    }
}