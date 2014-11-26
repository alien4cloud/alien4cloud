package alien4cloud.configuration;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import alien4cloud.Constants;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.security.Role;
import alien4cloud.security.groups.GroupService;

import com.google.common.collect.Sets;

/**
 * Default internal settings about security (groups/users)
 */
@Slf4j
@Component
public class DefaultGroup {

    private final String DESCRIPTION = "A internal group representing all alien users.";

    @Resource
    private GroupService groupService;

    @PostConstruct
    public void createDefaultAllGroup() {
        try {
            String createdGroupId = groupService.createGroup(Constants.GROUP_NAME_ALL_USERS, null, DESCRIPTION,
                    Sets.newHashSet(Role.COMPONENTS_BROWSER.toString()), null);
            if (createdGroupId != null) {
                log.info("Default group <{}> created in your system with id <{}>", Constants.GROUP_NAME_ALL_USERS, createdGroupId);
            }
        } catch (AlreadyExistException e) {
            log.info("Default group <{}> already exists in your system", Constants.GROUP_NAME_ALL_USERS);
        }
    }
}
