package alien4cloud.security.groups;

import static alien4cloud.utils.AlienUtils.safe;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.InvalidArgumentException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.security.event.GroupDeletedEvent;
import alien4cloud.security.event.UserDeletedEvent;
import alien4cloud.security.groups.rest.UpdateGroupRequest;
import alien4cloud.security.model.Group;
import alien4cloud.security.model.Role;
import alien4cloud.security.model.User;
import alien4cloud.security.users.UserService;
import alien4cloud.utils.ReflectionUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class GroupService {

    @Resource
    private IAlienGroupDao alienGroupDao;

    @Resource
    private UserService userService;

    @Inject
    private ApplicationEventPublisher publisher;

    public void updateGroup(String groupId, UpdateGroupRequest groupUpdateRequest) {
        Group group = retrieveGroup(groupId);
        String currentGroupName = group.getName();
        ReflectionUtil.mergeObject(groupUpdateRequest, group);
        if (group.getName() == null || group.getName().isEmpty()) {
            throw new InvalidArgumentException("Group's name cannot be set to null or empty");
        }
        // Check with precedent value
        if (!currentGroupName.equals(group.getName())) {
            // If group name has changed, must check unicity
            checkGroupNameUniqueness(group.getName());
        }
        alienGroupDao.save(group);
    }

    public void deleteGroup(String groupId) throws IOException, ClassNotFoundException {
        Group group = retrieveGroup(groupId);
        alienGroupDao.delete(groupId);
        publisher.publishEvent(new GroupDeletedEvent(this, group));
    }

    public User addUserToGroup(String username, String groupId) {
        Group group = retrieveGroup(groupId);
        User user = userService.retrieveUser(username);

        Set<String> users = group.getUsers() == null ? new HashSet<String>() : group.getUsers();
        users.add(user.getUsername());
        group.setUsers(users);

        alienGroupDao.save(group);

        // update groupRoles in the user
        userService.addGroupToUser(group, user);
        return user;
    }

    public User removeUserFromGroup(String username, String groupId) {
        Group group = retrieveGroup(groupId);
        if (CollectionUtils.isEmpty(group.getUsers())) {
            return null;
        }

        User user = userService.retrieveUser(username);

        group.getUsers().remove(user.getUsername());
        alienGroupDao.save(group);

        // update groupRoles in users objects
        userService.removeGroupFromUser(user, group);
        return user;
    }

    public String createGroup(String name, String email, String description, Set<String> roles, Set<String> users) throws AlreadyExistException {
        checkGroupNameUniqueness(name);
        Group group = new Group(name);
        group.setId(UUID.randomUUID().toString());
        group.setDescription(description);
        group.setEmail(email);
        if (CollectionUtils.isNotEmpty(roles)) {
            Set<String> formatedRoles = Sets.newHashSet();
            for (String role : roles) {
                formatedRoles.add(Role.getStringFormatedRole(role));
            }
            group.setRoles(formatedRoles);
        }

        List<User> usersList = null;
        if (CollectionUtils.isNotEmpty(users)) {
            usersList = Lists.newArrayList();
            Set<String> usersSet = Sets.newHashSet();
            for (String username : users) {
                usersList.add(userService.retrieveUser(username));
                usersSet.add(username);
            }
            group.setUsers(usersSet);
        }

        alienGroupDao.save(group);

        if (CollectionUtils.isNotEmpty(usersList)) {
            for (User user : usersList) {
                userService.addGroupToUser(group, user);
            }
        }

        return group.getId();
    }

    public void addRoleToGroup(String groupId, String role) {
        Group group = retrieveGroup(groupId);

        Set<String> rolesSet = group.getRoles() == null ? new HashSet<String>() : group.getRoles();
        rolesSet.add(Role.getStringFormatedRole(role));
        group.setRoles(rolesSet);

        alienGroupDao.save(group);

        // update groupRoles in users objects
        if (CollectionUtils.isNotEmpty(group.getUsers())) {
            for (String username : group.getUsers()) {
                userService.addGroupRoleToUser(username, role);
            }
        }
    }

    public Group retrieveGroup(String id) {
        Group group = alienGroupDao.find(id);
        if (group == null) {
            throw new NotFoundException("Group [" + id + "] cannot be found");
        }
        return group;
    }

    public void removeRoleFromGroup(String groupId, String role) {
        Group group = retrieveGroup(groupId);
        if (CollectionUtils.isEmpty(group.getRoles())) {
            return;
        }

        group.getRoles().remove(Role.getStringFormatedRole(role));
        alienGroupDao.save(group);

        // update groupRoles in users objects
        if (CollectionUtils.isNotEmpty(group.getUsers())) {
            for (String username : group.getUsers()) {
                userService.updateUserGroupRoles(username);
            }
        }

    }

    /**
     * check if the given groupId is unique in the system
     * 
     * @param groupName
     */
    private void checkGroupNameUniqueness(String groupName) throws AlreadyExistException {
        if (alienGroupDao.isGroupWithNameExist(groupName)) {
            log.debug("Create group [ {} ] impossible (already exists)", groupName);
            // a group already exist with the given id.
            throw new AlreadyExistException("A Group with the given id <" + groupName + "> already exists.");
        } else {
            log.debug("Create group [ {} ]", groupName);
        }
    }

    /**
     * Listener for user deleted. Removes the user from all groups where he was
     * 
     * @param event
     */
    @EventListener
    public void userDeletedEventListener(UserDeletedEvent event) {
        User user = event.getUser();
        for (String groupId : safe(user.getGroups())) {
            Group group = retrieveGroup(groupId);
            if (safe(group.getUsers()).contains(user.getUsername())) {
                group.getUsers().remove(user.getUsername());
                alienGroupDao.save(group);
            }
        }
    }

}
