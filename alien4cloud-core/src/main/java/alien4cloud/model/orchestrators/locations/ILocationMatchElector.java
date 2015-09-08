package alien4cloud.model.orchestrators.locations;

/**
 * Elector for a locationMatch object
 */
public interface ILocationMatchElector {

    /**
     * Given a locationMatch, check if it is eligible according to some conditions
     *
     * @param locationMatch
     * @return true if the conditions are passed, false is not
     */
    boolean isEligible(LocationMatch locationMatch);
}
