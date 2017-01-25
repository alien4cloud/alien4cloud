package alien4cloud.rest.application;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.paas.model.DeploymentStatus;
import alien4cloud.rest.application.model.ApplicationEnvironmentDTO;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * Creates an ApplicationEnvironmentDTO from an ApplicationEnvironment adding the deployment status informations.
 */
@Slf4j
@Component
public class ApplicationEnvironmentDTOBuilder {
    @Resource
    private ApplicationEnvironmentService applicationEnvironmentService;

    /**
     * Get a list a application environment DTO
     *
     * @param applicationEnvironments
     * @return
     */
    public ApplicationEnvironmentDTO[] getApplicationEnvironmentDTO(ApplicationEnvironment[] applicationEnvironments) {
        List<ApplicationEnvironmentDTO> listApplicationEnvironmentsDTO = Lists.newArrayList();
        for (ApplicationEnvironment env : applicationEnvironments) {
            listApplicationEnvironmentsDTO.add(getApplicationEnvironmentDTO(env));
        }
        return listApplicationEnvironmentsDTO.toArray(new ApplicationEnvironmentDTO[listApplicationEnvironmentsDTO.size()]);
    }

    /**
     * Creates an ApplicationEnvironmentDTO from an ApplicationEnvironment adding the deployment status informations.
     * 
     * @param env The environment for which to create DTO
     * @return The application environement DTO matching the given application environement.
     */
    public ApplicationEnvironmentDTO getApplicationEnvironmentDTO(ApplicationEnvironment env) {
        ApplicationEnvironmentDTO tempEnvDTO = new ApplicationEnvironmentDTO();
        tempEnvDTO.setApplicationId(env.getApplicationId());
        tempEnvDTO.setDescription(env.getDescription());
        tempEnvDTO.setEnvironmentType(env.getEnvironmentType());
        tempEnvDTO.setId(env.getId());
        tempEnvDTO.setName(env.getName());
        tempEnvDTO.setUserRoles(env.getUserRoles());
        tempEnvDTO.setGroupRoles(env.getGroupRoles());
        tempEnvDTO.setCurrentVersionName(env.getTopologyVersion());
        try {
            Deployment deployment = applicationEnvironmentService.getActiveDeployment(env.getId());
            tempEnvDTO.setStatus(applicationEnvironmentService.getStatus(deployment));
            if (!DeploymentStatus.UNDEPLOYED.equals(tempEnvDTO.getStatus())) {
                tempEnvDTO.setDeployedVersion(deployment.getVersionId());
            }
        } catch (Exception e) {
            log.debug("Getting status for the environment <" + env.getId()
                    + "> failed because the associated orchestrator cannot be reached. Returned status is UNKNOWN.", e);
            tempEnvDTO.setStatus(DeploymentStatus.UNKNOWN);
        }
        return tempEnvDTO;
    }
}
