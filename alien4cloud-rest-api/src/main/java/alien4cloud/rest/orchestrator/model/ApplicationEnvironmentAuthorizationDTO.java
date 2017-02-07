package alien4cloud.rest.orchestrator.model;

import java.util.List;
import java.util.Map;

import org.elasticsearch.common.collect.Lists;

import com.google.common.collect.Maps;

import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * This DTO represents an authorization given to a given application / environment.
 * When environments is empty, this means that the whole application has persmission (all it's environments).
 */
@Getter
@Setter
@AllArgsConstructor
public class ApplicationEnvironmentAuthorizationDTO {

    private Application application;

    private List<ApplicationEnvironment> environments;

    public static List<ApplicationEnvironmentAuthorizationDTO> buildDTOs(List<Application> applicationsRelatedToEnvironment, List<ApplicationEnvironment> environments, List<Application> applications) {
        Map<String, ApplicationEnvironmentAuthorizationDTO> aeaDTOsMap = Maps.newHashMap();
        if (!environments.isEmpty()) {
            applicationsRelatedToEnvironment.stream().forEach(application -> aeaDTOsMap.put(application.getId(), new ApplicationEnvironmentAuthorizationDTO(application, Lists.newArrayList())));
            for (ApplicationEnvironment ae : environments) {
                ApplicationEnvironmentAuthorizationDTO dto = aeaDTOsMap.get(ae.getApplicationId());
                dto.getEnvironments().add(ae);
            }
        }
        if (!applications.isEmpty()) {
            for (Application application : applications) {
                ApplicationEnvironmentAuthorizationDTO dto = aeaDTOsMap.get(application.getId());
                if (dto == null) {
                    aeaDTOsMap.put(application.getId(), new ApplicationEnvironmentAuthorizationDTO(application, null));
                } else {
                    // the application has detailed environment authorizations but the whole application authorization has precedence.
                    dto.setEnvironments(null);
                }
            }
        }
        return Lists.newArrayList(aeaDTOsMap.values());
    }
}
