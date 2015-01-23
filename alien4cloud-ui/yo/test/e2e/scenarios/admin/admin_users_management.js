/* global by, element */

'use strict';

var common = require('../../common/common');
var authentication = require('../../authentication/authentication');
var navigation = require('../../common/navigation');
var rolesCommon = require('../../common/roles_common');
var users = require('../../admin/users');

var assertUserExists = function(username, exists) {
  expect(element(by.id('user_' + username)).isPresent()).toBe(exists);
  if (exists) {
    expect(element(by.id('user_' + username)).element(by.binding('user.username')).getText()).toEqual(username);
  }
};

var assertUserHasNoRoles = function(username) {
  expect(element(by.id('user_' + username)).isElementPresent(by.css('ul.td_list'))).toBe(false);
};

describe('User management', function() {
  // Load up a view and wait for it to be done with its rendering and epicycles.
  beforeEach(function() {
    common.before();
  });

  afterEach(function() {
    // Logout action
    common.after();
  });

  it('should be able to go to users search page', function() {
    console.log('################# should be able to go to users search page ');
    authentication.login('admin');

    //go to the users search page
    users.navigationUsers();

    // the user list table
    expect(element(by.id('users-table')).isPresent()).toBe(true, 'The user table should should be present when we reach the user list page.');
  });

  it('should be able to create/delete a new user, and find it in the users list', function() {
    console.log('################# should be able to create a new user, and find it in the users list');
    //login
    authentication.login('admin');

    //go to the users search page
    users.navigationUsers();

    //check new user button
    expect(element(by.binding('USERS.NEW')).isPresent()).toBe(true);

    //create and check a user
    users.createUser(authentication.users.sauron);
    assertUserExists(authentication.users.sauron.username, true);

    users.deleteUser(authentication.users.sauron.username);
    navigation.go('main', 'admin');
    navigation.go('admin', 'users');
    assertUserExists(authentication.users.sauron.username, false);
  });

  it('should be able to see and edit user\'s roles', function() {
    console.log('################# should be able to see and edit user\'s roles');
    //login
    authentication.login('admin');

    //create a user and check no roles for now
    users.createUser(authentication.users.bilbo);
    assertUserHasNoRoles(authentication.users.bilbo.username);

    //add roles to bilbo
    rolesCommon.editUserRole(authentication.users.bilbo.username, rolesCommon.alienRoles.admin);
    rolesCommon.assertUserHasRoles(authentication.users.bilbo.username, rolesCommon.alienRoles.admin);
    rolesCommon.editUserRole(authentication.users.bilbo.username, rolesCommon.alienRoles.componentsBrowser);
    rolesCommon.assertUserHasRoles(authentication.users.bilbo.username, rolesCommon.alienRoles.componentsBrowser);

    //refresh the page and check again
    navigation.go('main', 'admin');
    navigation.go('admin', 'users');
    rolesCommon.assertUserHasRoles(authentication.users.bilbo.username, rolesCommon.alienRoles.admin);
    rolesCommon.assertUserHasRoles(authentication.users.bilbo.username, rolesCommon.alienRoles.componentsBrowser);

    //remove roles from bilbo
    rolesCommon.editUserRole(authentication.users.bilbo.username, rolesCommon.alienRoles.admin);
    rolesCommon.assertUserDoesNotHaveRoles(authentication.users.bilbo.username, rolesCommon.alienRoles.admin);
    navigation.go('main', 'admin');
    navigation.go('admin', 'users');
    rolesCommon.assertUserDoesNotHaveRoles(authentication.users.bilbo.username, rolesCommon.alienRoles.admin);
  });

  it('should be able to edit user\'s properties and fields', function() {
    console.log('should be able to edit user\'s properties and fields');
    // login
    authentication.login('admin');

    // create a user
    navigation.go('main', 'admin');
    navigation.go('admin', 'users');
    users.createUser(authentication.users.sauron);

    // edit the first name field
    var firstName = 'Sauron';
    common.sendValueToXEditable('user_' + authentication.users.sauron.username + '_firstName', firstName);
    navigation.go('main', 'admin');
    navigation.go('admin', 'users');
    common.expectValueFromXEditable('user_' + authentication.users.sauron.username + '_firstName', firstName);

    // edit the last name field
    var lastName = 'the Maia';
    common.sendValueToXEditable('user_' + authentication.users.sauron.username + '_lastName', lastName);
    navigation.go('main', 'admin');
    navigation.go('admin', 'users');
    common.expectValueFromXEditable('user_' + authentication.users.sauron.username + '_lastName', lastName);

    //edit the email field
    var newEmail = 'sauron@mordor.nz';
    common.sendValueToXEditable('user_' + authentication.users.sauron.username + '_email', newEmail);
    navigation.go('main', 'admin');
    navigation.go('admin', 'users');
    common.expectValueFromXEditable('user_' + authentication.users.sauron.username + '_email', newEmail);
  });
});
