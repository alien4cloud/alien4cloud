package alien4cloud.security;

/**
 * Objects that implements {@link IManagedSecuredResource} is a secured resource that delegates the security informations handling to another
 * {@link ISecuredResource}.
 */
public interface IManagedSecuredResource {
    /**
     * Get the id of the {@link ISecuredResource} that manages the current resource's security.
     * 
     * @return The id of the {@link ISecuredResource} that manages the current resource's security.
     */
    String getDelegateId();

    /**
     * Get the type of the secured resource that manages the current resource's security.
     * 
     * @return The type of the secured resource that manages the current resource's security.
     */
    String getDelegateType();
}