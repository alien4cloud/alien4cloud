/* global element, by */

'use strict';

var authentication = require('../authentication/authentication');
var common = require('../common/common');
var navigation = require('../common/navigation');
var applications = require('../applications/applications');
var topologyEditorCommon = require('../topology/topology_editor_common');
var cloudsCommon = require('../admin/clouds_common');
var componentData = require('../topology/component_data');

describe('Disabling / Enabling cloud and application when deployed: ', function() {

  beforeEach(function() {
    topologyEditorCommon.beforeTopologyTest();
    //deploy an applicaton
    applications.deploy('Alien', { compute: componentData.toscaBaseTypes.compute() });
  });

  /* After each spec in the tests suite(s) */
  afterEach(function() {
    // Logout action
    common.after();
  });

  it('should not be able to disable or delete a cloud when used for a deployment', function() {
    console.log('################# should not be able to disable or delete a cloud when used for a deployment');
    authentication.reLogin('admin');
    cloudsCommon.goToCloudList();
    cloudsCommon.goToCloudDetail('testcloud');
    cloudsCommon.disableCloud();
    cloudsCommon.checkCloudError(true);
    cloudsCommon.deleteCloud();
    cloudsCommon.checkCloudError(true);
  });

  it('should not be able to delete an application when deployed', function() {
    console.log('################# should not be able to delete an application when deployed');
    navigation.go('main', 'applications');

    //delete from app list
    common.deleteWithConfirm('delete-app_Alien', true);
    expect(element(by.id('app_Alien')).isPresent()).toBe(true);
    // remove the toaster
    common.dismissAlert();

    //delete from app detail
    applications.goToApplicationDetailPage('Alien');
    common.deleteWithConfirm('btn-delete-app', true);

    //  Should be more specific to apps
    common.expectErrors();
    common.dismissAlert();
  });

  it('should be able to see deployments on a cloud', function() {
    console.log('################# should be able to see deployments on a cloud');
    authentication.reLogin('admin');
    cloudsCommon.goToCloudList();
    cloudsCommon.goToCloudDetail('testcloud');
    var deploymentsDiv = element(by.id('deployments-div'));
    expect(deploymentsDiv.all(by.repeater('deploymentDTO in deployments')).count()).toEqual(1);
    cloudsCommon.checkDeploymentsDisplayed(['Alien'], true);
  });
});
