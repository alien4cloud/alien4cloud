package alien4cloud.common;

import alien4cloud.json.deserializer.AbstractDiscriminatorPolymorphicDeserializer;
import alien4cloud.model.orchestrators.locations.AbstractLocationResourceTemplate;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.model.orchestrators.locations.PolicyLocationResourceTemplate;

/**
 * Custom deserializer to handle multiple Location resources types.
 */
public class LocationResourceDeserializer extends AbstractDiscriminatorPolymorphicDeserializer<AbstractLocationResourceTemplate> {

    public LocationResourceDeserializer() {
        super(AbstractLocationResourceTemplate.class);
        addToRegistry("generated", LocationResourceTemplate.class);
        setDefaultClass(PolicyLocationResourceTemplate.class);
    }

}