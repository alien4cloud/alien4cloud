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
    applications.deploy('Alien', {
      compute: componentData.toscaBaseTypes.compute()
    }, null, null, applications.mockPaaSDeploymentProperties);
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

  it('should not be able to update a cloud for an deployed environment', function() {
    console.log('################# should not be able to update a cloud for an deployed environment');
    // 'Alien' application is deployed check cloud list disabled
    var selectCloud = element(by.id('cloud-select'));
    browser.sleep(8000); // Technical sleep (deployment in progress)
    expect(selectCloud.isDisplayed()).toBe(true);
    expect(selectCloud.isEnabled()).not.toBe(true); // cloud list disabled
  });


  it('should be able to set deployment properties for 2 environments and check them for each environment', function() {
    console.log('################# should be able to set deployment properties for 2 environments and check them for each environment');
    // 'Alien' application is deployed check cloud list disabled

    // check properties before undeploy
    applications.expectDeploymentPropertyValue('p_managementUrl', applications.mockPaaSDeploymentProperties.managementUrl, false);
    applications.expectDeploymentPropertyValue('p_managerEmail', applications.mockPaaSDeploymentProperties.managerEmail, false);
    applications.expectDeploymentPropertyValue('p_numberBackup', applications.mockPaaSDeploymentProperties.numberBackup, false);

    // change submenu
    navigation.go('applications', 'info');

    // undeploy the app an check properties
    applications.undeploy(); // will jump to deployment state
    applications.expectDeploymentPropertyValue('p_managementUrl', applications.mockPaaSDeploymentProperties.managementUrl, true);
    applications.expectDeploymentPropertyValue('p_managerEmail', applications.mockPaaSDeploymentProperties.managerEmail, true);
    applications.expectDeploymentPropertyValue('p_numberBackup', applications.mockPaaSDeploymentProperties.numberBackup, true);

    // switch environment en check differents property definitions
    // 1 ENV =>  1 Deployment Setup => 1 deployment property list
    applications.createApplicationEnvironment('ENV-MORDOR', 'A new environment for the mordor...', 'testcloud', applications.environmentTypes.other, '0.1.0-SNAPSHOT');
    applications.setupDeploymentProperties('Alien', 'ENV-MORDOR', 'testcloud', applications.mockDeploymentPropertiesMordor);

    navigation.go('applications', 'info');
    navigation.go('applications', 'deployment');
    applications.switchEnvironmentAndCloud('ENV-MORDOR', null);
    applications.expectDeploymentPropertyValue('p_managementUrl', applications.mockDeploymentPropertiesMordor.managementUrl, true);
    applications.expectDeploymentPropertyValue('p_managerEmail', applications.mockDeploymentPropertiesMordor.managerEmail, true);
    applications.expectDeploymentPropertyValue('p_numberBackup', applications.mockDeploymentPropertiesMordor.numberBackup, true);

    applications.simpleDeploy();
    // after deployment, properties are no longer editable
    applications.expectDeploymentPropertyValue('p_managementUrl', applications.mockDeploymentPropertiesMordor.managementUrl, false);
    applications.expectDeploymentPropertyValue('p_managerEmail', applications.mockDeploymentPropertiesMordor.managerEmail, false);
    applications.expectDeploymentPropertyValue('p_numberBackup', applications.mockDeploymentPropertiesMordor.numberBackup, false);

    // change back to default 'Environment', which is undeployed, and check that properties are editable
    applications.switchEnvironmentAndCloud('Environment', null);
    applications.expectDeploymentPropertyValue('p_managementUrl', applications.mockPaaSDeploymentProperties.managementUrl, true);
    applications.expectDeploymentPropertyValue('p_managerEmail', applications.mockPaaSDeploymentProperties.managerEmail, true);
    applications.expectDeploymentPropertyValue('p_numberBackup', applications.mockPaaSDeploymentProperties.numberBackup, true);

  });

  it('should have deployment properties edition disabled for a deployed environment', function() {
    console.log('################# should have deployment properties edition disabled for a deployed environment');
    // Alien application is deployed
    // we're on deployment page > check deployment properties disabled
    applications.expectDeploymentPropertyValue('p_managementUrl', applications.mockPaaSDeploymentProperties.managementUrl, false);
    applications.expectDeploymentPropertyValue('p_managerEmail', applications.mockPaaSDeploymentProperties.managerEmail, false);
    applications.expectDeploymentPropertyValue('p_numberBackup', applications.mockPaaSDeploymentProperties.numberBackup, false);
  });

});
