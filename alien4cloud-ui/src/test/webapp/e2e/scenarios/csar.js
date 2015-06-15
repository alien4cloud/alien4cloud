/* global element, by */
'use strict';

var common = require('../common/common');
var authentication = require('../authentication/authentication');
var csarCommon = require('../csars/csars_commons');

describe('Handle CSARS', function() {

  beforeEach(function() {
    common.before();
    authentication.reLogin(authentication.users.componentManager.username, authentication.users.componentManager.password);
  });

  /* After each spec in the tests suite(s) */
  afterEach(function() {
    // Logout action
    common.after();
  });

  it('should not be able to delete a CSAR referenced by an application / csars /  topologytemplate', function() {
    console.log('################# should not be able to delete a CSAR referenced by an application / csars /  topologytemplate');

    csarCommon.goToCsarSearchPage();

    // check the csar count
    var results = element.all(by.repeater('csar in csarSearchResult.data.data'));
    expect(results.count()).toEqual(6);

    // jump to tosca-base-types csars and check errors and check the "linked resources list"
    csarCommon.goToCsarDetails(0);
    expect(element(by.binding('csar.csar.name')).getText()).toContain('tosca-base-types');
    expect(element(by.binding('csar.csar.version')).getText()).toContain('1.0');
    results = element.all(by.repeater('resource in csar.relatedResources'));
    expect(results.count()).toEqual(4);

    // try to delete the tosca-base-types csars and check errors
    common.deleteWithConfirm('delete-csar_tosca-base-types:1.0', true);
    common.expectErrors();
    common.expectMessageContent('java-types');
    common.expectMessageContent('apacheLB');
    common.expectMessageContent('ubuntu-types');

  });

  it('should not be able to delete a CSAR referenced by an application / csars /  topologytemplate from CSAR list', function() {
    console.log('################# should not be able to delete a CSAR referenced by an application / csars /  topologytemplate from CSAR list');

    csarCommon.goToCsarSearchPage();

    // check the csar count : 6 csars remaining
    var results = element.all(by.repeater('csar in csarSearchResult.data.data'));
    expect(results.count()).toEqual(6);

    // try to delete the tosca-base-types csars and check errors
    common.deleteWithConfirm('delete-csar_tosca-base-types:1.0', true);
    common.expectErrors();
    common.expectMessageContent('java-types');
    common.expectMessageContent('apacheLB');
    common.expectMessageContent('ubuntu-types');

    // check the csar count : still 6
    expect(results.count()).toEqual(6);

  });

  it('should be able to delete an uploaded and non referenced CSAR', function() {
    console.log('################# should be able to delete an uploaded and non referenced CSAR');

    // jump to java-types csar details
    csarCommon.goToCsarSearchPage();
    csarCommon.goToCsarDetails(1);
    expect(element(by.binding('csar.csar.name')).getText()).toContain('java-types');
    expect(element(by.binding('csar.csar.version')).getText()).toContain('1.0');
    var resourceList = element.all(by.repeater('resource in csar.relatedResources'));
    expect(resourceList.count()).toEqual(0);

    // try to delete the tosca-base-types csars and check errors
    common.deleteWithConfirm('delete-csar_java-types:1.0', true);
    common.expectNoErrors();

    // check the csar count : from 6 to 5
    var csarsList = element.all(by.repeater('csar in csarSearchResult.data.data'));
    expect(csarsList.count()).toEqual(5);

  });

});
