/* global by, element */

'use strict';

//Roles
var alienRoles = {
  admin: 'ADMIN',
  applicationsManager: 'APPLICATIONS_MANAGER',
  componentsManager: 'COMPONENTS_MANAGER',
  componentsBrowser: 'COMPONENTS_BROWSER'
};
module.exports.alienRoles = alienRoles;

//application Roles
var appRoles = {
  appManager: 'APPLICATION_MANAGER',
  appDevops: 'APPLICATION_DEVOPS'
};
module.exports.appRoles = appRoles;

var envRoles = {
  deploymentManager: 'DEPLOYMENT_MANAGER',
  envUser: 'APPLICATION_USER'
}
module.exports.envRoles = envRoles;

// Orchestrator Roles
var orchestratorRoles = {
  orchestratorDeployer: 'ORCHESTRATOR_DEPLOYER'
};
module.exports.orchestratorRoles = orchestratorRoles;

// Cloud Roles
var cloudRoles = {
  cloudDeployer: 'CLOUD_DEPLOYER'
};
module.exports.cloudRoles = cloudRoles;

function editRole(appOrEnv, userOrGroup, name, role) {
  var editRolesBtn = element(by.id(userOrGroup + '_' + name)).element(by.id('edit-' + appOrEnv + '-' + userOrGroup + '-role-button'));
  editRolesBtn.click();
  var roleLink = element(by.id(name + '_' + role));
  roleLink.click();
  editRolesBtn.click();
}

function assertRoleChecked(appOrEnv, userOrGroup, name, role, checked) {
  var editRolesBtn = element(by.id(userOrGroup + '_' + name)).element(by.id('edit-' + appOrEnv + '-' + userOrGroup + '-role-button'));
  editRolesBtn.click();
  if (checked) {
    expect(element(by.id(name + '_' + role)).getAttribute('class')).toContain('checked_role');
  } else {
    expect(element(by.id(name + '_' + role)).getAttribute('class')).not.toContain('checked_role');
  }
  editRolesBtn.click();
}

function assertGroupChecked(appOrEnv, username, groupName, checked) {
  var editGroupsBtn = element(by.id('user_' + username)).element(by.id('edit-' + appOrEnv + '-group-role-button'));
  editGroupsBtn.click();
  if (checked) {
    expect(element(by.id(username + '_' + groupName)).getAttribute('class')).toContain('checked_role');
  } else {
    expect(element(by.id(username + '_' + groupName)).getAttribute('class')).not.toContain('checked_role');
  }
  editGroupsBtn.click();
}

module.exports.editUserRole = function(username, role) {
  editRole('app', 'user', username, role);
};

module.exports.editUserRoleForAnEnv = function(username, role) {
  editRole('env', 'user', username, role);
};

var editGroupRole = function(username, role) {
  editRole('app', 'group', username, role);
};
module.exports.editGroupRole = editGroupRole;
module.exports.editGroupRoleForAnApp = editGroupRole;

module.exports.editGroupRoleForAnEnv = function(username, role) {
  editRole('env', 'group', username, role);
};

var toggleUserGroup = function(appOrEnv, username, groupName) {
  var editGroupsBtn = element(by.id('user_' + username)).element(by.id('edit-' + appOrEnv +'-group-role-button'));
  editGroupsBtn.click();
  var roleLink = element(by.id(username + '_' + groupName));
  roleLink.click();
  editGroupsBtn.click();
};
module.exports.addUserToGroup = function(username, groupName) {
  toggleUserGroup('app', username, groupName);
}

module.exports.removeUserFromGroup = function(username, groupName) {
  toggleUserGroup('app', username, groupName);
}
// This method test specific roles assigned to user without taken into account roles given to user's group
var assertUserHasRoles = function(appOrEnv, username, roles) {
  if (!Array.isArray(roles)) {
    roles = [roles];
  }
  roles.forEach(function(role) {
    assertRoleChecked(appOrEnv, 'user', username, role, true);
  });
};
module.exports.assertUserHasRoles = function(username, groups) {
  assertUserHasRoles('app', username, groups);
};
module.exports.assertUserHasRolesForAnEnv = function(username, groups) {
  assertUserHasRoles('env', username, groups);
};

// This method test specific roles assigned to user without taken into account roles given to user's group
var assertUserDoesNotHaveRoles = function(username, roles) {
  if (!Array.isArray(roles)) {
    roles = [roles];
  }
  roles.forEach(function(role) {
    assertRoleChecked('app', 'user', username, role, false);
  });
};
module.exports.assertUserDoesNotHaveRoles = assertUserDoesNotHaveRoles;

module.exports.assertUserHasGroups = function(appOrEnv, username, groups) {
  if (!Array.isArray(groups)) {
    groups = [groups];
  }
  groups.forEach(function(group) {
    assertGroupChecked(appOrEnv, username, group, true);
    expect(element(by.id('user_' + username)).element(by.name('groups')).getText()).toContain(group);
  });
};

var assertGroupHasRoles = function(appOrEnv, groupName, roles) {
  if (!Array.isArray(roles)) {
    roles = [roles];
  }
  for (var i = 0; i < roles.length; i++) {
    assertRoleChecked(appOrEnv, 'group', groupName, roles[i], true);
  }
};
module.exports.assertGroupHasRoles = assertGroupHasRoles;

var assertGroupDoesNotHaveRoles = function(appOrEnv, groupName, roles) {
  if (!Array.isArray(roles)) {
    roles = [roles];
  }
  roles.forEach(function(role) {
    assertRoleChecked(appOrEnv, 'group', groupName, role, false);
  });
};
module.exports.assertGroupDoesNotHaveRoles = assertGroupDoesNotHaveRoles;
