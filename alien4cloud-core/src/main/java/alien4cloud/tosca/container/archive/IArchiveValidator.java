package alien4cloud.tosca.container.archive;

import alien4cloud.tosca.container.model.CloudServiceArchive;
import alien4cloud.tosca.container.validation.CSARValidationResult;

public interface IArchiveValidator {

    /**
     * Validate the archive.
     * 
     * @param cloudServiceArchive archive to be validated.
     * @return validation result which contains constraint violations if any, compilation errors if any.
     */
    CSARValidationResult validateArchive(CloudServiceArchive cloudServiceARchive);
}
