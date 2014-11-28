/**
 * CSAR functionality is not enabled for now...
 */

'use strict';

var common = require('../common/common');
var authentication = require('../authentication/authentication');
var genericForm = require('../generic_form/generic_form');
var csarCommon = require('../csars/csars_commons');

describe('Handle CSARS', function() {

  beforeEach(function() {
    common.before();
    authentication.login(authentication.users.componentManager.username, authentication.users.componentManager.password);
  });

  /* After each spec in the tests suite(s) */
  afterEach(function() {
    // Logout action
    common.after();
  });

  it('should be able to delete an uploaded SNAPSHOT CSAR (TODO : UPDATE WITH CSAR UI CHANGES)', function() {
    console.log('################# should be able to delete an uploaded SNAPSHOT CSAR');
    // csarCommon.goToCsarSearchPage();
    // csarCommon.goToCsarDetails(1);
    // expect(element(by.binding('csar.name')).getText()).toContain(csar1.name);
    // expect(element(by.binding('csar.version')).getText()).toContain(csar1.version);
    // expect(element(by.binding('csar.description')).getText()).toContain(csar1.description);
  });

});
