/* global by, element, expect */

'use strict';

var common = require('./common');

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
};
module.exports.envRoles = envRoles;

// Orchestrator Roles
var orchestratorRoles = {
  orchestratorDeployer: 'ORCHESTRATOR_DEPLOYER'
};
module.exports.orchestratorRoles = orchestratorRoles;

function editRole(appOrEnv, userOrGroup, name, role) {
  var target = common.element(by.id(userOrGroup + '_' + name));
  common.click(by.id('edit-' + appOrEnv + '-' + userOrGroup + '-role-button'), target);
  common.click(by.id(name + '_' + role));
  common.click(by.id('edit-' + appOrEnv + '-' + userOrGroup + '-role-button'), target);
}

function assertRoleChecked(appOrEnv, userOrGroup, name, role, checked) {
  var target = common.element(by.id(userOrGroup + '_' + name));
  common.click(by.id('edit-' + appOrEnv + '-' + userOrGroup + '-role-button'), target);
  if (checked) {
    expect(element(by.id(name + '_' + role)).getAttribute('class')).toContain('checked_role');
  } else {
    expect(element(by.id(name + '_' + role)).getAttribute('class')).not.toContain('checked_role');
  }
  common.click(by.id('edit-' + appOrEnv + '-' + userOrGroup + '-role-button'), target);
}

// This method test specific roles assigned to user without taken into account roles given to user's group
function assertUserHasRoles(appOrEnv, username, roles) {
  if (!Array.isArray(roles)) {
    roles = [roles];
  }
  roles.forEach(function(role) {
    assertRoleChecked(appOrEnv, 'user', username, role, true);
  });
}
// This method test specific roles assigned to user without taken into account roles given to user's group
function assertUserDoesNotHaveRoles(appOrEnv, username, roles) {
  if (!Array.isArray(roles)) {
    roles = [roles];
  }
  roles.forEach(function(role) {
    assertRoleChecked(appOrEnv, 'user', username, role, false);
  });
}

function toggleUserGroup(appOrEnv, username, groupName) {
  var target = common.element(by.id('user_' + username));
  common.click(by.id('edit-' + appOrEnv + '-group-role-button'), target);
  common.click(by.id(username + '_' + groupName));
  common.click(by.id('edit-' + appOrEnv + '-group-role-button'), target);
}

function assertGroupChecked(appOrEnv, username, groupName, checked) {
  var target = common.element(by.id('user_' + username));
  common.click(by.id('edit-' + appOrEnv + '-group-role-button'), target);
  if (checked) {
    expect(element(by.id(username + '_' + groupName)).getAttribute('class')).toContain('checked_role');
  } else {
    expect(element(by.id(username + '_' + groupName)).getAttribute('class')).not.toContain('checked_role');
  }
  common.click(by.id('edit-' + appOrEnv + '-group-role-button'), target);
}

module.exports.editUserRole = function(username, role) {
  editRole('app', 'user', username, role);
};
module.exports.editUserRoleForEnv = function(username, role) {
  editRole('env', 'user', username, role);
};
module.exports.editGroupRole = function(username, role) {
  editRole('app', 'group', username, role);
};
module.exports.editGroupRoleForEnv = function(username, role) {
  editRole('env', 'group', username, role);
};

module.exports.addUserToGroup = function(username, groupName) {
  toggleUserGroup('app', username, groupName);
};

module.exports.removeUserFromGroup = function(username, groupName) {
  toggleUserGroup('app', username, groupName);
};

module.exports.assertUserHasRoles = function(username, role) {
  assertUserHasRoles('app', username, role);
};

module.exports.assertUserDoesNotHaveRoles = function(username, role) {
  assertUserDoesNotHaveRoles('app', username, role);
};

module.exports.assertUserHasRolesForEnv = function(username, roles) {
  assertUserHasRoles('env', username, roles);
};

module.exports.assertUserDoesNotHaveRolesForEnv = function(username, roles) {
  assertUserDoesNotHaveRoles('env', username, roles);
};

module.exports.assertUserHasGroups = function(appOrEnv, username, groups) {
  if (!Array.isArray(groups)) {
    groups = [groups];
  }
  groups.forEach(function(group) {
    assertGroupChecked(appOrEnv, username, group, true);
    expect(element(by.id('user_' + username)).element(by.name('groups')).getText()).toContain(group);
  });
};

function assertGroupHasRoles(appOrEnv, groupName, roles) {
  if (!Array.isArray(roles)) {
    roles = [roles];
  }
  for (var i = 0; i < roles.length; i++) {
    assertRoleChecked(appOrEnv, 'group', groupName, roles[i], true);
  }
}

module.exports.assertGroupHasRoles = function(groupName, roles) {
  assertGroupHasRoles('app', groupName, roles);
};

module.exports.assertGroupHasRolesForEnv = function(groupName, roles) {
  assertGroupHasRoles('env', groupName, roles);
};

function assertGroupDoesNotHaveRoles(appOrEnv, groupName, roles) {
  if (!Array.isArray(roles)) {
    roles = [roles];
  }
  roles.forEach(function(role) {
    assertRoleChecked(appOrEnv, 'group', groupName, role, false);
  });
}

module.exports.assertGroupDoesNotHaveRoles = function(groupName, roles) {
  return assertGroupDoesNotHaveRoles('app', groupName, roles);
};

module.exports.assertGroupDoesNotHaveRolesForEnv = function(groupName, roles) {
  assertGroupDoesNotHaveRoles('env', groupName, roles);
};
