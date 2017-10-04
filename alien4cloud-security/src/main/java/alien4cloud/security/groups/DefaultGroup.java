package alien4cloud.security.groups;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import alien4cloud.exception.AlreadyExistException;
import alien4cloud.security.model.Role;
import alien4cloud.utils.AlienConstants;
import lombok.extern.slf4j.Slf4j;

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
            String createdGroupId = groupService.createGroup(AlienConstants.GROUP_NAME_ALL_USERS, null, DESCRIPTION,
                    Sets.newHashSet(Role.COMPONENTS_BROWSER.toString()), null);
            if (createdGroupId != null) {
                log.info("Default group [ {} ] created in your system with id [ {} ]", AlienConstants.GROUP_NAME_ALL_USERS, createdGroupId);
            }
        } catch (AlreadyExistException e) {
            log.info("Default group [ {} ] already exists in your system", AlienConstants.GROUP_NAME_ALL_USERS);
        }
    }
}