package alien4cloud.initialization;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import alien4cloud.exception.AlreadyExistException;
import alien4cloud.security.groups.GroupService;

/**
 * Default internal settings about security (groups/users)
 * 
 * @author mourouvi
 *
 */
@Slf4j
@Component
public class Security {

    public static final String GROUP_NAME_ALL_USERS = "ALL";
    private final String DESCRIPTION = "A internal group representing all alien users.";

    @Resource
    private GroupService groupService;

    @PostConstruct
    public void createDefaulAlltGroup() {
        try {
            String createdGroupId = groupService.createGroup(GROUP_NAME_ALL_USERS, null, DESCRIPTION, null, null);
            if (createdGroupId != null) {
                log.info("Default group <{}> created in your system with id <{}>", GROUP_NAME_ALL_USERS, createdGroupId);
            }
        } catch (AlreadyExistException e) {
            log.info("Default group <{}> already exists in your system", GROUP_NAME_ALL_USERS);
        }

    }

}
