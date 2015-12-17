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

  it('Admin should be able to see all applications and the create button and check pagination', function() {
    authentication.login('admin');
    applications.go();
    expect(element(by.id('app-new-btn')).isPresent()).toBe(true);
    var pages = element.all(by.repeater('page in pages'));
    expect(pages.count()).toBe(6);
  });

  it('Admin should be able to remove an application on which he has no roles', function() {
    var currentAppName = 'ApplicationTestUIDelete';
    applications.searchApplication(currentAppName);
    common.deleteWithConfirm('delete-app_' + currentAppName, true);
    toaster.expectNoErrors();
  });

  it('Architect should be able to see it\'s own application and not the create button', function() {
    var currentAppName = 'AlienUITestArchitect';
    authentication.logout();
    authentication.login('architect');
    applications.go();
    expect(element(by.id('app-new-btn')).isPresent()).toBe(false);
    applications.searchApplication(currentAppName);
    expect(element(by.id('app_' + currentAppName)).isPresent()).toBe(true);
  });

  it('User should be able to see it\'s own application and not the create button', function() {
    var currentAppName = 'AlienUITestUser';
    authentication.logout();
    authentication.login('user');
    applications.go();
    expect(element(by.id('app-new-btn')).isPresent()).toBe(false);
    applications.searchApplication(currentAppName);
    expect(element(by.id('app_' + currentAppName)).isPresent()).toBe(true);
  });

  it('Application manager should be able to see it\'s own application and the create button', function() {
    var currentAppName = 'AlienUITestManager';
    authentication.logout();
    authentication.login('applicationManager');
    applications.go();
    expect(element(by.id('app-new-btn')).isPresent()).toBe(true);
    applications.searchApplication(currentAppName);
    expect(element(by.id('app_' + currentAppName)).isPresent()).toBe(true);
  });

  it('Application manager should be able to add a new application with unique name', function() {
    var currentAppName = 'AlienUITestManagerCreate';
    applications.createApplication(currentAppName, 'Great Application');
    toaster.expectNoErrors();
    applications.go();
    applications.searchApplication(currentAppName);
    expect(element(by.id('app_' + currentAppName)).isPresent()).toBe(true);
  });

  it('Application manager should not be able to add a new topology template with existing name', function() {
    var currentAppName = 'AlienUITestManagerCreate';
    applications.createApplication(currentAppName, 'Great Application');
    toaster.expectErrors();
    toaster.dismissIfPresent();
  });

  it('Application manager be able to remove an application he own', function() {
    var currentAppName = 'AlienUITestManagerCreate';
    common.deleteWithConfirm('delete-app_' + currentAppName, true);
    toaster.expectNoErrors();
  });

  it('Application manager should not be able to remove an application he is user for', function() {
    var currentAppName = 'AlienUITestManagerDeleteFailed';
    common.deleteWithConfirm('delete-app_' + currentAppName, true);
    toaster.expectErrors();
  });

  // it('Application manager should be able to create a new application from an existing topology and check that the topology tab has a not empty topology.', function() {
  //   var currentAppName = ' AlienUITestManagerCreateTopology';
  //   applications.createApplication(currentAppName, 'Great Application', 2);
  //   // go to topology and check is not empty
  // });

  it('afterAll', function() { authentication.logout(); });
});
