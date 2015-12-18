package alien4cloud.security.spring.saml;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.saml.key.KeyManager;
import org.springframework.security.saml.metadata.ExtendedMetadata;
import org.springframework.security.saml.metadata.MetadataGenerator;
import org.springframework.stereotype.Component;

/**
 * Configure the generator
 */
@Component
@ConditionalOnProperty(value = "saml.enabled", havingValue = "true")
public class ServiceProviderMetadataGenerator extends MetadataGenerator {
    @Inject
    private KeyManager keyManager;
    @Inject
    private ExtendedMetadata extendedMetadata;

    @PostConstruct
    public void init() {
        setIncludeDiscoveryExtension(false);
        setExtendedMetadata(extendedMetadata);
        setKeyManager(keyManager);
    }

    @Required
    @Value("${saml.metadata.sp.entityId}")
    @Override
    public void setEntityId(String entityId) {
        super.setEntityId(entityId);
    }

    @Override
    @Value("${saml.metadata.sp.entityBaseURL:http://localhost:8088}")
    public void setEntityBaseURL(String entityBaseURL) {
        super.setEntityBaseURL(entityBaseURL);
    }

    @Override
    @Value("${saml.metadata.sp.requestSigned:false}")
    public void setRequestSigned(boolean requestSigned) {
        super.setRequestSigned(requestSigned);
    }

    @Override
    @Value("${saml.metadata.sp.wantAssertionSigned:false}")
    public void setWantAssertionSigned(boolean wantAssertionSigned) {
        super.setWantAssertionSigned(wantAssertionSigned);
    }
}