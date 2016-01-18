/* global describe, it, beforeEach, by, element, expect */

'use strict';

var setup = require('../../common/setup');
var common = require('../../common/common');
var xedit = require('../../common/xedit');
var authentication = require('../../authentication/authentication');
var rolesCommon = require('../../common/roles_common.js');
var users = require('../../admin/users');

function assertGroupExists(groupName, exists) {
  expect(element(by.id('group_' + groupName)).isPresent()).toBe(exists);
  if (exists) {
    expect(element(by.id('group_' + groupName)).element(by.binding('group.name')).getText()).toEqual(groupName);
  }
}

function assertGroupHasNoRoles(groupName) {
  expect(element(by.id('group_' + groupName)).isElementPresent(by.css('ul.td_list'))).toBe(false);
}

function assertUserHasRoles(username, roles) {
  if (!Array.isArray(roles)) {
    roles = [roles];
  }
  roles.forEach(function(role) {
    expect(element(by.id('user_' + username)).element(by.name('roles')).getText()).toContain(role);
  });
}

/*
* used to check if the user has a role from direct assignement or from a group.
* @param username The name of the user.
* @param roles The roles to check.
* @param groupRole If true this means we expect the role from a group, if false from a direct assignement.
* @param exists If true we check that the user has the role, if not that he doesn't have the role.
*/
function assertUserHasRolesFrom(username, roles, groupRole, exists) {
  if (!Array.isArray(roles)) {
    roles = [roles];
  }

  roles.forEach(function(role) {
    var roleElements = common.element(by.name('roles'),common.element(by.id('user_' + username)));
    roleElements.all(by.repeater('(role, from) in user.allRoles')).then(function(roleItems) {
      roleItems.forEach(function(roleElement) {
        var searchedClass = groupRole ? 'fa-users' : 'fa-user';
        roleElement.getText().then(function(text) {
          if(text.indexOf(role) > -1) {
            expect(roleElement.element(by.className(searchedClass)).isPresent()).toBe(exists);
          }
        });
      });
    });
  });
}

function assertGroupHasRoles(groupname, roles) {
  if (!Array.isArray(roles)) {
    roles = [roles];
  }
  roles.forEach(function(role) {
    expect(element(by.id('group_' + groupname)).element(by.name('roles')).getText()).toContain(role);
  });
  rolesCommon.assertGroupHasRoles(groupname, roles);
}

function assertGroupDoesNotHaveRoles(groupname, roles) {
  if (!Array.isArray(roles)) {
    roles = [roles];
  }
  roles.forEach(function(role) {
    expect(element(by.id('group_' + groupname)).element(by.name('roles')).getText()).not.toContain(role);
  });
  rolesCommon.assertGroupDoesNotHaveRoles(groupname, roles);
}

function jumpToTab(tabName) {
  element(by.id(tabName + '-tab')).element(by.tagName('a')).click();
}

describe('Group management', function() {
  // Load up a view and wait for it to be done with its rendering and epicycles.
  it('beforeAll', function() {
    common.home();
    authentication.login('admin');
  });

  beforeEach(function(){
    setup.setup(); // reset data.
  });

  it('should be able to create a new group, and find it in the group list', function() {
    users.goToGroups();

    expect(common.element(by.id('groups-table')).isPresent()).toBe(true);
    expect(common.element(by.binding('GROUPS.NEW')).isPresent()).toBe(true);

    //create and check a group
    users.createGroup(users.groups.managers);
    assertGroupExists(users.groups.managers.name, true);
  });

  it('should be able to see and edit group\'s roles', function() {
    users.goToGroups();

    users.createGroup(users.groups.managers);
    assertGroupHasNoRoles(users.groups.managers.name);

    //add roles to managers
    rolesCommon.editGroupRole(users.groups.managers.name, rolesCommon.alienRoles.applicationsManager);
    assertGroupHasRoles(users.groups.managers.name, rolesCommon.alienRoles.applicationsManager);
    rolesCommon.editGroupRole(users.groups.managers.name, rolesCommon.alienRoles.componentsManager);
    assertGroupHasRoles(users.groups.managers.name, rolesCommon.alienRoles.componentsManager);

    //refresh the page and check again
    users.goToGroups();
    assertGroupHasRoles(users.groups.managers.name, rolesCommon.alienRoles.applicationsManager);
    assertGroupHasRoles(users.groups.managers.name, rolesCommon.alienRoles.componentsManager);

    //remove roles from managers
    rolesCommon.editGroupRole(users.groups.managers.name, rolesCommon.alienRoles.componentsManager);
    assertGroupDoesNotHaveRoles(users.groups.managers.name, rolesCommon.alienRoles.componentsManager);
    users.goToGroups();
    assertGroupDoesNotHaveRoles(users.groups.managers.name, rolesCommon.alienRoles.componentsManager);
  });

  it('should be able to delete a group', function() {
    users.goToGroups();

    users.createGroup(users.groups.managers);
    assertGroupExists(users.groups.managers.name, true);
    users.createGroup(users.groups.architects);
    assertGroupExists(users.groups.architects.name, true);

    //delete architects group
    users.deleteGroup(users.groups.architects.name);
    assertGroupExists(users.groups.architects.name, false);

    //refresh the page and check again
    users.goToGroups();
    assertGroupExists(users.groups.architects.name, false);
  });

  it('should be able to edit group\'s properties and fields', function() {
    users.goToGroups();

    users.createGroup(users.groups.managers);
    assertGroupExists(users.groups.managers.name, true);

    //edit the description field
    var description = 'This is the description';
    xedit.sendKeys('group_' + users.groups.managers.name + '_description', description, false, 'textarea');
    xedit.expect('group_' + users.groups.managers.name + '_description', description);

    //edit the email field
    var newEmail = 'obama@whitehouse.us';
    xedit.sendKeys('group_' + users.groups.managers.name + '_email', newEmail);
    xedit.expect('group_' + users.groups.managers.name + '_email', newEmail);

    //edit the name field
    var newName = 'ManagersS';
    xedit.sendKeys('group_' + users.groups.managers.name + '_name', newName);
    users.goToGroups();
    xedit.expect('group_' + newName + '_name', newName);
  });


  it('should be able to update users info when modifying a group', function() {
    // create groups
    users.goToGroups();
    users.createGroup(users.groups.managers);
    assertGroupExists(users.groups.managers.name, true);

    // add APPLICATION_MANAGER roles to managers group
    rolesCommon.editGroupRole(users.groups.managers.name, rolesCommon.alienRoles.applicationsManager);
    assertGroupHasRoles(users.groups.managers.name, rolesCommon.alienRoles.applicationsManager);

    // Create sauron
    jumpToTab('users');
    users.createUser(authentication.users.sauron);
    // add sauron to Managers group (should have APPLICATION_MANAGER role)
    rolesCommon.addUserToGroup(authentication.users.sauron.username, users.groups.managers.name);
    rolesCommon.assertUserHasGroups('app', authentication.users.sauron.username, users.groups.managers.name);
    assertUserHasRoles(authentication.users.sauron.username, rolesCommon.alienRoles.applicationsManager);
    assertUserHasRolesFrom(authentication.users.sauron.username, rolesCommon.alienRoles.applicationsManager, true, true);

    // add a role to managers group and check users roles
    jumpToTab('groups');
    rolesCommon.editGroupRole(users.groups.managers.name, rolesCommon.alienRoles.componentsManager);
    assertGroupHasRoles(users.groups.managers.name, [rolesCommon.alienRoles.componentsManager, rolesCommon.alienRoles.applicationsManager]);

    jumpToTab('users');
    assertUserHasRoles(authentication.users.sauron.username, [rolesCommon.alienRoles.componentsManager, rolesCommon.alienRoles.applicationsManager]);
    // sauron is COMPONENTS_MANAGER and APPLICATIONS_MANAGER from group assignment but NOT from direct assignment.
    assertUserHasRolesFrom(authentication.users.sauron.username, [rolesCommon.alienRoles.componentsManager, rolesCommon.alienRoles.applicationsManager], true, true);
    assertUserHasRolesFrom(authentication.users.sauron.username, [rolesCommon.alienRoles.componentsManager, rolesCommon.alienRoles.applicationsManager], false, false);

    // sauron is directly assigned to COMPONENTS_MANAGER
    rolesCommon.editUserRole(authentication.users.sauron.username, rolesCommon.alienRoles.componentsManager);

    // the user has the role COMPONENTS_MANAGER from the group and by himself now
    assertUserHasRoles(authentication.users.sauron.username, [rolesCommon.alienRoles.componentsManager, rolesCommon.alienRoles.applicationsManager]);
    assertUserHasRolesFrom(authentication.users.sauron.username, rolesCommon.alienRoles.componentsManager, true, true);
    assertUserHasRolesFrom(authentication.users.sauron.username, rolesCommon.alienRoles.componentsManager, false, true);


    jumpToTab('groups');
    users.deleteGroup(users.groups.managers.name);
    assertGroupExists(users.groups.managers.name, false);
    jumpToTab('users');
    assertUserHasRoles(authentication.users.sauron.username, rolesCommon.alienRoles.componentsManager);
    rolesCommon.assertUserDoesNotHaveRoles(authentication.users.sauron.username, rolesCommon.alienRoles.applicationsManager);
    assertUserHasRolesFrom(authentication.users.sauron.username, rolesCommon.alienRoles.componentsManager, false, true);
  });

  it('afterAll', function() { authentication.logout(); });
});
