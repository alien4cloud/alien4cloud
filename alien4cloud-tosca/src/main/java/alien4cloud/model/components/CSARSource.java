package alien4cloud.model.components;

/**
 * Enumeration of alien csars sources.
 */
public enum CSARSource {
    /** Alien out of the box archives. */
    ALIEN,
    /** Orchestrator archives. */
    ORCHESTRATOR,
    /** Generated from topology substitution. */
    TOPOLOGY_SUBSTITUTION,
    /** Manual upload. */
    UPLOAD,
    /** Git import. */
    GIT,
    /** Archive embedded in plugin. */
    PLUGIN,
    /** Other source. */
    OTHER
}
