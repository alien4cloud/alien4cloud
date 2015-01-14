/* global by, element */

'use strict';

var authentication = require('../authentication/authentication');
var common = require('../common/common');
var rolesCommon = require('../common/roles_common.js');
var users = require('../admin/users');

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

function assertUserHasRolesFrom(username, roles, fromType, exists) {

  if (!Array.isArray(roles)) {
    roles = [roles];
  }

  roles.forEach(function(role) {
    element(by.id('user_' + username)).element(by.name('roles')).then(function(role) {
      var roleRepeater = element.all(by.repeater('(role, from) in user.allRoles'));
      roleRepeater.then(function(roleItems) {
        roleItems.forEach(function(role) {
          var current = role.element(by.tagName('i'));
          role.getText().then(function(text) {
            if (roles.indexOf(text) != -1) {
              var searhedClass = (fromType == 'g') ? 'fa-users' : 'fa-user';
              if (!exists) {
                expect(current.getAttribute('class')).not.toEndsWith(searhedClass);
              } else {
                expect(current.getAttribute('class')).toEndsWith(searhedClass);
              }
            }
          });
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
  rolesCommon.assertGroupHasRoles('app', groupname, roles);
}


function assertGroupDoesNotHaveRoles(groupname, roles) {
  if (!Array.isArray(roles)) {
    roles = [roles];
  }
  roles.forEach(function(role) {
    expect(element(by.id('group_' + groupname)).element(by.name('roles')).getText()).not.toContain(role);
  });
  rolesCommon.assertGroupDoesNotHaveRoles('app', groupname, roles);
}

function jumpToTab(tabName) {
  element(by.id(tabName + '-tab')).element(by.tagName('a')).click();
}

describe('Group management', function() {
  // Load up a view and wait for it to be done with its rendering and epicycles.
  beforeEach(function() {

    common.before();

    // add specific matcher
    this.addMatchers({
      toEndsWith: function(toFindAtEnd) {
        var regex = new RegExp(toFindAtEnd + '$');
        return this.actual.match(regex) != null ? true : false;
      }
    });

  });

  afterEach(function() {
    // Logout action
    common.after();
  });

  it('should be able to create a new group, and find it in the group list', function() {
    console.log('################# should be able to create a new group, and find it in the group list');

    authentication.login('admin');

    //go to the group search page
    users.navigationGroups();

    expect(element(by.id('groups-table')).isPresent()).toBe(true);

    //check new group button
    expect(element(by.binding('GROUPS.NEW')).isPresent()).toBe(true);

    //create and check a group
    users.createGroup(users.groups.managers);
    assertGroupExists(users.groups.managers.name, true);
  });

  it('should be able to see and edit group\'s roles', function() {
    console.log('################# should be able to see and edit group\'s roles');
    //login
    authentication.login('admin');

    //create a user and check no roles for now
    users.navigationGroups();
    users.createGroup(users.groups.managers);
    assertGroupHasNoRoles(users.groups.managers.name);

    //add roles to managers
    rolesCommon.editGroupRole('app', users.groups.managers.name, rolesCommon.alienRoles.applicationsManager);
    assertGroupHasRoles(users.groups.managers.name, rolesCommon.alienRoles.applicationsManager);
    rolesCommon.editGroupRole('app', users.groups.managers.name, rolesCommon.alienRoles.componentsManager);
    assertGroupHasRoles(users.groups.managers.name, rolesCommon.alienRoles.componentsManager);

    //refresh the page and check again
    users.navigationGroups();
    assertGroupHasRoles(users.groups.managers.name, rolesCommon.alienRoles.applicationsManager);
    assertGroupHasRoles(users.groups.managers.name, rolesCommon.alienRoles.componentsManager);

    //remove roles from managers
    rolesCommon.editGroupRole('app', users.groups.managers.name, rolesCommon.alienRoles.componentsManager);
    assertGroupDoesNotHaveRoles(users.groups.managers.name, rolesCommon.alienRoles.componentsManager);
    users.navigationGroups();
    assertGroupDoesNotHaveRoles(users.groups.managers.name, rolesCommon.alienRoles.componentsManager);
  });

  it('should be able to delete a group', function() {
    console.log('################# should be able to delete a group');
    //login
    authentication.login('admin');

    //create a group
    users.navigationGroups();
    users.createGroup(users.groups.managers);
    assertGroupExists(users.groups.managers.name, true);
    users.createGroup(users.groups.architects);
    assertGroupExists(users.groups.architects.name, true);

    //delete architects group
    users.deleteGroup(users.groups.architects.name);
    assertGroupExists(users.groups.architects.name, false);

    //refresh the page and check again
    users.navigationGroups();
    assertGroupExists(users.groups.architects.name, false);
  });

  it('should be able to edit group\'s properties and fields', function() {
    console.log('################# should be able to edit group\'s properties and fields');
    //login
    authentication.login('admin');

    //create a group
    users.navigationGroups();
    users.createGroup(users.groups.managers);
    assertGroupExists(users.groups.managers.name, true);

    //edit the description field
    var description = 'This is the description';
    common.sendValueToXEditable('group_' + users.groups.managers.name + '_description', description, false, 'textarea');
    users.navigationGroups();
    common.expectValueFromXEditable('group_' + users.groups.managers.name + '_description', description);

    //edit the email field
    var newEmail = 'obama@whitehouse.us';
    common.sendValueToXEditable('group_' + users.groups.managers.name + '_email', newEmail);
    users.navigationGroups();
    common.expectValueFromXEditable('group_' + users.groups.managers.name + '_email', newEmail);

    //edit the name field
    var newName = 'ManagersS';
    common.sendValueToXEditable('group_' + users.groups.managers.name + '_name', newName);
    users.navigationGroups();
    common.expectValueFromXEditable('group_' + newName + '_name', newName);
  });


  it('should be able to update users info when modifying a group', function() {

    console.log('################# should be able to update users info when modifying a group');
    // login
    authentication.login('admin');

    // create groups
    users.navigationGroups();
    users.createGroup(users.groups.managers);
    assertGroupExists(users.groups.managers.name, true);

    // add roles to managers
    rolesCommon.editGroupRole('app', users.groups.managers.name, rolesCommon.alienRoles.applicationsManager);
    assertGroupHasRoles(users.groups.managers.name, rolesCommon.alienRoles.applicationsManager);

    // create and check a user
    jumpToTab('users');
    users.createUser(authentication.users.sauron);

    // add sauron to Managers group
    rolesCommon.addUserToGroup('app', authentication.users.sauron.username, users.groups.managers.name);
    rolesCommon.assertUserHasGroups('app', authentication.users.sauron.username, users.groups.managers.name);
    assertUserHasRoles(authentication.users.sauron.username, rolesCommon.alienRoles.applicationsManager);
    assertUserHasRolesFrom(authentication.users.sauron.username, rolesCommon.alienRoles.componentsManager, 'g', true);

    // add a role to managers group and check users roles
    jumpToTab('groups');
    rolesCommon.editGroupRole('app', users.groups.managers.name, rolesCommon.alienRoles.componentsManager);
    rolesCommon.editGroupRole('app', users.groups.managers.name, rolesCommon.alienRoles.admin);
    assertGroupHasRoles(users.groups.managers.name, [rolesCommon.alienRoles.componentsManager, rolesCommon.alienRoles.applicationsManager]);

    jumpToTab('users');
    assertUserHasRoles(authentication.users.sauron.username, [rolesCommon.alienRoles.componentsManager, rolesCommon.alienRoles.applicationsManager]);
    // sauron user hase 2 roles granted by 'Manager' group
    assertUserHasRolesFrom(authentication.users.sauron.username, [rolesCommon.alienRoles.componentsManager, rolesCommon.alienRoles.applicationsManager], 'g', true);

    // check roles list icons
    // the user has no COMPONENTS_MANAGER / APPLICATIONS_MANAGER by himself
    assertUserHasRolesFrom(authentication.users.sauron.username, rolesCommon.alienRoles.componentsManager, 'u', false);
    assertUserHasRolesFrom(authentication.users.sauron.username, rolesCommon.alienRoles.applicationsManager, 'u', false);

    //add a role to the user, delete the group and check user's roles
    rolesCommon.editUserRole('app', authentication.users.sauron.username, rolesCommon.alienRoles.componentsManager);

    // the user has the role COMPONENTS_MANAGER from the group and by himself now
    assertUserHasRolesFrom(authentication.users.sauron.username, rolesCommon.alienRoles.componentsManager, 'g', true);
    assertUserHasRolesFrom(authentication.users.sauron.username, rolesCommon.alienRoles.componentsManager, 'u', true);
    assertUserHasRoles(authentication.users.sauron.username, [rolesCommon.alienRoles.componentsManager, rolesCommon.alienRoles.applicationsManager]);

    jumpToTab('groups');
    users.deleteGroup(users.groups.managers.name);
    assertGroupExists(users.groups.managers.name, false);
    jumpToTab('users');
    assertUserHasRoles(authentication.users.sauron.username, rolesCommon.alienRoles.componentsManager);
    rolesCommon.assertUserDoesNotHaveRoles(authentication.users.sauron.username, rolesCommon.alienRoles.applicationsManager);
    assertUserHasRolesFrom(authentication.users.sauron.username, rolesCommon.alienRoles.componentsManager, 'u', true);

  });

});
