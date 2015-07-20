/* global element, by */
'use strict';

var common = require('../../common/common');
var authentication = require('../../authentication/authentication');
var csarCommon = require('../../csars/csars_commons');

describe('Handle CSARS', function() {

  beforeEach(function() {
    common.before();
    authentication.reLogin(authentication.users.admin.username, authentication.users.admin.password);
  });

  /* After each spec in the tests suite(s) */
  afterEach(function() {
    // Logout action
    authentication.logout();
  });

  it('should be able to add a new the csar by the modal', function() {
    console.log('################# should be able to add a new the csar by the modal');
    csarCommon.checkIfCreationStepIsEnabled('https://github.com/alien4cloud/tosca-normative-types','','master');
    var results = element.all(by.repeater('csar in csarGitSearchResult.data.data'));
    expect(results.count()).toEqual(1);
1
  });

  it('should not be able to add a new the csar by the modal if data are not correct', function() {
    console.log('################# should not be able to add a new the csar by the modal if data are not correct');
    csarCommon.checkIfCreationIsDisabled('wrong/samples','','master');
    var createCsarGitButton = element(by.id('btn-create'));
    expect(createCsarGitButton.isEnabled()).toBe(false);
    var results = element.all(by.repeater('csar in csarGitSearchResult.data.data'));
    expect(results.count()).toEqual(1);
  });

  it('should not be able to add a new the csar by the modal if locations is empty', function() {
    console.log('################# should not be able to add a new the csar by the modal if locations is empty');
    csarCommon.checkIfAllCreationStepsAreDisabled('https://github.com/alien4cloud/samples','','');
    var results = element.all(by.repeater('csar in csarGitSearchResult.data.data'));
    expect(results.count()).toEqual(1);
  });

  it('should be able to import a csargit', function() {
    console.log('################# should be able to import a csargit');
    csarCommon.goToCsarSearchPage();
    var results = element.all(by.repeater('csar in csarSearchResult.data.data'));
    var lenght;
    var sizeBefore=results.count().then(function(countBefore){
      lenght=countBefore;
    });
    var importButton=element(by.id('IMPORT_CSARGIT'));
    importButton.click();
    var length=results.count().then(function(count){
      expect(lenght+1).toEqual(count);
    });
  });

  it('should be able to delete a csargit', function() {
    console.log('################# should be able to delete a csargit');
    var results = element.all(by.repeater('csar in csarGitSearchResult.data.data'));
    csarCommon.goToCsarSearchPage();
    common.deleteWithConfirm('delete-csargit', true);
    var results = element.all(by.repeater('csar in csarGitSearchResult.data.data'));
    expect(results.count()).toEqual(0);
  });
});
