package alien4cloud.security.users;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;

import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.security.groups.IAlienGroupDao;
import alien4cloud.security.model.Group;
import alien4cloud.security.model.Role;
import alien4cloud.security.model.User;
import alien4cloud.security.users.rest.UpdateUserRequest;
import alien4cloud.utils.ReflectionUtil;

import com.google.common.collect.Sets;

@Component
public class UserService {

    @Resource
    private IAlienUserDao alienUserDao;
    @Resource
    private IAlienGroupDao alienGroupDao;

    @Value("${alien_security.admin.ensure}")
    private boolean ensure;

    @Value("${alien_security.admin.username}")
    private String adminUserName;

    @Value("${alien_security.admin.password}")
    private String adminPassword;

    @Value("${alien_security.admin.email}")
    private String email;

    /** Ensure that there is at least one user with admin role. */
    @PostConstruct
    public void ensureAdminUser() {
        if (!ensure) {
            return;
        }
        // we should have one at least one user with role ADMIN
        Map<String, String[]> filters = new HashMap<String, String[]>();
        filters.put("roles", new String[] { Role.ADMIN.toString() });
        GetMultipleDataResult searchResult = alienUserDao.find(filters, 1);

        // if no user with admin role can be found, then creates a new one.
        if (searchResult != null && searchResult.getTotalResults() == 0) {
            adminUserName = (adminUserName == null) ? "admin" : adminUserName;
            adminPassword = (adminPassword == null) ? "admin" : adminPassword;

            String[] roles = new String[] { Role.ADMIN.toString() };
            createUser(adminUserName, email, null, null, roles, adminPassword);
        }
    }

    @SuppressWarnings("rawtypes")
    public int countAdminUser() {
        Map<String, String[]> filters = new HashMap<String, String[]>();
        filters.put("roles", new String[] { Role.ADMIN.toString() });
        GetMultipleDataResult searchResult = alienUserDao.find(filters, 1);
        return (int) searchResult.getTotalResults();
    }

    /**
     * Create a new internal user and saves it.
     * 
     * @param userName The userName of the new user.
     * @param email The email of the new user.
     * @param firstName The firstName of the new user.
     * @param lastName The lastname of the new user.
     * @param roles The roles of the new user.
     * @param password The password of the new user (hash only will be saved).
     */
    public void createUser(String userName, String email, String firstName, String lastName, String[] roles, String password) {
        if (alienUserDao.find(userName) != null) {
            throw new AlreadyExistException("A user with the given username already exists.");
        }
        User user = new User();
        user.setUsername(userName);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRoles(roles);
        user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        user.setInternalDirectory(true);
        user.setEnabled(true);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);
        alienUserDao.save(user);
    }

    /**
     * Update an user on a specific field name
     * 
     * @param userName name of the user
     * @param userUpdateRequest value of the field
     */
    public void updateUser(String userName, UpdateUserRequest userUpdateRequest) {
        User user = retrieveUser(userName);
        ReflectionUtil.mergeObject(userUpdateRequest, user);
        if (userUpdateRequest.getPassword() != null) {
            user.setPassword(BCrypt.hashpw(userUpdateRequest.getPassword(), BCrypt.gensalt()));
        }
        alienUserDao.save(user);
    }

    /**
     * retrieve a user if exists
     * 
     * @param username the username of the user to retrieve
     * @return the {@link User} if found.
     */
    public User retrieveUser(String username) {
        User user = alienUserDao.find(username);
        if (user == null) {
            throw new NotFoundException("User [" + username + "] cannot be found");
        }
        return user;
    }

    /**
     * Add a group to a user, including all the group roles
     * 
     * @param group The group to process
     * @param user The user to process
     */
    public void addGroupToUser(Group group, User user) {
        Set<String> groupSet = user.getGroups() == null ? new HashSet<String>() : user.getGroups();
        groupSet.add(group.getId());
        user.setGroups(groupSet);
        if (CollectionUtils.isNotEmpty(group.getRoles())) {
            Set<String> groupRolesSet = user.getGroupRoles() == null ? new HashSet<String>() : user.getGroupRoles();
            groupRolesSet.addAll(group.getRoles());
            user.setGroupRoles(groupRolesSet);
        }

        alienUserDao.save(user);

    }

    /**
     * Add a group to a user, including all the group roles
     * 
     * @param group The group to process
     * @param username The username of the user to process
     */
    public void addGroupToUser(Group group, String username) {
        addGroupToUser(group, retrieveUser(username));
    }

    /**
     * Add a group role to a user
     * 
     * @param role The group role to add
     * @param username The username of the user to process
     */
    public void addGroupRoleToUser(String username, String role) {
        User user = retrieveUser(username);
        Set<String> groupRolesSet = user.getGroupRoles() == null ? new HashSet<String>() : user.getGroupRoles();
        groupRolesSet.add(Role.getStringFormatedRole(role));
        user.setGroupRoles(groupRolesSet);

        alienUserDao.save(user);
    }

    public void saveUser(User user) {
        alienUserDao.save(user);
    }

    /**
     * 
     * Regenerate the group roles in the user.
     * 
     * @param user the user for which to regenerate the group roles
     */
    public void updateUserGroupRoles(User user) {
        if (CollectionUtils.isEmpty(user.getGroups())) {
            user.setGroupRoles(null);
        } else {
            Set<String> groupRolesSet = Sets.newHashSet();
            for (String groupId : user.getGroups()) {
                Group group = alienGroupDao.find(groupId);
                if (group == null) {
                    throw new NotFoundException("Group [" + groupId + "] cannot be found");
                }
                if (CollectionUtils.isNotEmpty(group.getRoles())) {
                    groupRolesSet.addAll(group.getRoles());
                }
            }
            user.setGroupRoles(groupRolesSet);
        }
        alienUserDao.save(user);
    }

    /**
     * 
     * Regenerate the group roles in the user.
     * 
     * @param username the username of the user for which to regenerate the group roles
     */
    public void updateUserGroupRoles(String username) {
        updateUserGroupRoles(retrieveUser(username));
    }

    /**
     * Remove a group from a user object
     * 
     * @param username the username of the user to process
     * @param group the group object to remove
     */
    public void removeGroupFromUser(String username, Group group) {
        User user = retrieveUser(username);
        removeGroupFromUser(user, group);
    }

    /**
     * Remove a group from a user object
     * 
     * @param user the user to process
     * @param group the group object to remove
     */
    public void removeGroupFromUser(User user, Group group) {
        if (CollectionUtils.isEmpty(user.getGroups())) {
            return;
        }
        user.getGroups().remove(group.getId());
        if (CollectionUtils.isNotEmpty(group.getRoles())) {
            updateUserGroupRoles(user);
        } else {
            alienUserDao.save(user);
        }
    }

    public boolean isAdmin(String username) {
        User user = retrieveUser(username);
        if (user.getRoles() == null) {
            return false;
        }
        ArrayList<String> roles = new ArrayList(Arrays.asList(user.getRoles()));
        return roles.contains("ADMIN");
    }

}
