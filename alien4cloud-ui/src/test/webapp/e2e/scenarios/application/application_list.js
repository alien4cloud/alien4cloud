/* global describe, it, by, element, expect */
'use strict';

var setup = require('../../common/setup');
var toaster = require('../../common/toaster');
var authentication = require('../../authentication/authentication');
var common = require('../../common/common');
var applications = require('../../applications/applications');

describe('Applications management list:', function() {
  it('beforeAll', function() {
    setup.setup();
    common.home();
  });

  // TODO: check the pagination and count the app
  it('Admin should be able to see all applications and the create button and check pagination', function() {
    authentication.login('admin');
    applications.go();
    expect(element(by.id('app-new-btn')).isPresent()).toBe(true);
  });

  it('Admin should be able to remove an application on which he has no roles', function() {
    applications.createApplication('AlienUITest', 'Great Application');
    common.deleteWithConfirm('delete-app_Application 1', true);
    toaster.expectNoErrors();
  });

  it('Architect should be able to see see it\'s own application and not the create button', function() {
    authentication.logout();
    authentication.login('architect');
    applications.go();
    expect(element(by.id('app-new-btn')).isPresent()).toBe(false);
  });

  it('User should be able to see see it\'s own application and not the create button', function() {
    authentication.logout();
    authentication.login('user');
    applications.go();
    expect(element(by.id('app-new-btn')).isPresent()).toBe(false);
  });

  it('Application manager should be able to see it\'s own application and the create button', function() {
    authentication.logout();
    authentication.login('applicationManager');
    applications.go();
    expect(element(by.id('app-new-btn')).isPresent()).toBe(true);
  });

  it('Application manager should be able to add a new application with unique name', function() {
    applications.createApplication('AlienUITest', 'Great Application');
    toaster.expectNoErrors();
  });

  it('Application manager should not be able to add a new topology template with existing name', function() {
    applications.createApplication('AlienUITest', 'Great Application');
    toaster.expectErrors();
  });

  it('Application manager be able to remove an application he own', function() {
    applications.createApplication('AlienUITest', 'Great Application');
    common.deleteWithConfirm('delete-app_Application 1', true);
    toaster.expectNoErrors();
  });

  it('Application manager should not be able to remove an application he is user for', function() {
    applications.createApplication('AlienUITest', 'Great Application');
    common.deleteWithConfirm('delete-app_Application 1', true);
    toaster.expectErrors();
  });

  it('Application manager should be able to create a new application from an existing topology and check that the topology tab has a not empty topology.', function() {
    applications.createApplication('AlienUITestWithTopology', 'Great Application', 2);
    // go to topology and check is not empty
  });

  it('afterAll', function() { authentication.logout(); });
});
