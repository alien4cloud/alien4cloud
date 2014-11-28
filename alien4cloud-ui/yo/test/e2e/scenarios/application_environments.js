/* global by, element */
'use strict';

var authentication = require('../authentication/authentication');
var common = require('../common/common');
var tagConfigCommon = require('../admin/metaprops_configuration_common');
var applications = require('../applications/applications');

describe('Application environments', function() {

  /* Before each spec in the tests suite */
  beforeEach(function() {
    common.before();
    authentication.login('applicationManager');
  });

  /* After each spec in the tests suite(s) */
  afterEach(function() {
    // Logout action
    common.after();
  });

  it('should create an application and must have a default application environment', function() {
    console.log('################# should create an application and must have a default application environment');

    applications.createApplication('Alien', 'Great Application');

  });

  xit('should create an application environment a new application', function() {
    console.log('################# should create an application environment a new application');
    applications.createApplication('Alien', 'Great Application');
  });

});
