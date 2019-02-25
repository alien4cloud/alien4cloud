package alien4cloud.rest.orchestrator.model;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.utils.ReflectionUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This DTO represents an authorization given to a given application / environment.
 * When environments is empty, this means that the whole application has persmission (all it's environments).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationEnvironmentAuthorizationDTO {

    private Application application;

    private List<ApplicationEnvironment> environments;

    private List<String> environmentTypes;

    public static GetMultipleDataResult<ApplicationEnvironmentAuthorizationDTO> convert(GetMultipleDataResult<Application> toConvert,
            List<ApplicationEnvironmentAuthorizationDTO> allDTOs) {
        if (toConvert == null) {
            return null;
        }
        GetMultipleDataResult<ApplicationEnvironmentAuthorizationDTO> converted = new GetMultipleDataResult<>();
        ReflectionUtil.mergeObject(toConvert, converted, "data");

        List<ApplicationEnvironmentAuthorizationDTO> data = Lists.newArrayList();
        for (Application app : toConvert.getData()) {
            for (ApplicationEnvironmentAuthorizationDTO appEnvDTO : allDTOs) {
                if (app.getId().equals(appEnvDTO.getApplication().getId())) {
                    data.add(appEnvDTO);
                }
            }
        }
        converted.setData(data.toArray(new ApplicationEnvironmentAuthorizationDTO[toConvert.getData().length]));
        return converted;
    }

    private static List<String> filterEnvType(String appId, List<String> environmentTypes) {
        List<String> envTypes = Lists.newArrayList();
        for (String envType : environmentTypes) {
            if (envType.split(":")[0].equals(appId)) {
                envTypes.add(envType.split(":")[1]);
            }
        }
        return envTypes;
    }

    public static List<ApplicationEnvironmentAuthorizationDTO> buildDTOs(List<Application> applicationsRelatedToEnvironment,
            List<Application> applicationsRelatedToEnvironmentType, List<ApplicationEnvironment> environments, List<Application> applications,
            List<String> environmentTypes) {
        Map<String, ApplicationEnvironmentAuthorizationDTO> aeaDTOsMap = Maps.newHashMap();
        if (!environments.isEmpty()) {
            applicationsRelatedToEnvironment.stream().forEach(application -> aeaDTOsMap.put(application.getId(),
                    new ApplicationEnvironmentAuthorizationDTO(application, Lists.newArrayList(), Lists.newArrayList())));
            for (ApplicationEnvironment ae : environments) {
                ApplicationEnvironmentAuthorizationDTO dto = aeaDTOsMap.get(ae.getApplicationId());
                dto.getEnvironments().add(ae);
            }
        }

        if (!environmentTypes.isEmpty()) {
            for (Application application : applicationsRelatedToEnvironmentType) {
                if (aeaDTOsMap.get(application.getId()) != null) {
                    aeaDTOsMap.get(application.getId()).getEnvironmentTypes().addAll(filterEnvType(application.getId(), environmentTypes));
                } else {
                    aeaDTOsMap.put(application.getId(), new ApplicationEnvironmentAuthorizationDTO(application, Lists.newArrayList(),
                            filterEnvType(application.getId(), environmentTypes)));
                }
            }
        }

        if (!applications.isEmpty()) {
            for (Application application : applications) {
                ApplicationEnvironmentAuthorizationDTO dto = aeaDTOsMap.get(application.getId());
                if (dto == null) {
                    aeaDTOsMap.put(application.getId(), new ApplicationEnvironmentAuthorizationDTO(application, null, null));
                } else {
                    // the application has detailed environment authorizations but the whole application authorization has precedence.
                    dto.setEnvironments(null);
                }
            }
        }
        return Lists.newArrayList(aeaDTOsMap.values());
    }
}
