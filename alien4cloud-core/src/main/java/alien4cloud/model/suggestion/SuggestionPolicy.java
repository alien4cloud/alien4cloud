package alien4cloud.model.suggestion;

/**
 * Define the behavior of the system when a user types something that is not suggested.
 */
public enum SuggestionPolicy {
    /** You can only choose one in results. */
    Strict,
    /** Ask for the user what to do (Old mode). */
    Ask,
    /** Add the new entry without asking the user. */
    Add,
    /** Don't ask for nothing, accept the value, but don't add it to registry (Default mode). */
    Accept
}
