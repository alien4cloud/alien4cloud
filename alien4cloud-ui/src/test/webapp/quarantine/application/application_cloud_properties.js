'use strict';

var authentication = require('../../authentication/authentication');
var common = require('../../common/common');
var applications = require('../../applications/applications');
var cloudsCommon = require('../../admin/clouds_common');
var componentData = require('../../topology/component_data');
var navigation = require('../../common/navigation');
var applications = require('../../applications/applications');
var topologyEditorCommon = require('../../topology/topology_editor_common');

describe('Application cloud properties', function() {

  /* Before each spec in the tests suite */
  beforeEach(function() {
    common.before();
    authentication.login('admin');

    cloudsCommon.goToCloudList();
    cloudsCommon.createNewCloud('testcloud');
    cloudsCommon.goToCloudDetail('testcloud');
    cloudsCommon.enableCloud();

    applications.createApplication('Alien', 'Great Application with application version...');
    applications.goToApplicationTopologyPage();
    topologyEditorCommon.addNodeTemplatesCenterAndZoom(componentData.verySimpleTopology.nodes);
    topologyEditorCommon.editNodeProperty('Compute', 'os_arch', 'x86_64');
    topologyEditorCommon.editNodeProperty('Compute', 'os_type', 'linux');
    topologyEditorCommon.editNodeProperty('Compute', 'containee_types', 'test', 'cap');
  });

  /* After each spec in the tests suite(s) */
  afterEach(function() {
    authentication.logout();
  });

  /* Test to ensure a non regression of bug describe in ALIEN-727 */
  it('should set the Cloud properties for different environments and versions', function() {
    console.log('################# should set the Cloud properties for different environments and versions.');
    applications.goToApplicationVersionPageForApp('Alien');
    applications.createApplicationVersion('0.2.0-SNAPSHOT', 'A new version for my application...', '0.1.0-SNAPSHOT');
    applications.goToApplicationEnvironmentPageForApp('Alien');
    applications.createApplicationEnvironment('ENV', 'A new environment for my application...', 'testcloud', applications.environmentTypes.dev, '0.2.0-SNAPSHOT');
    navigation.go('applications', 'deployment');

    applications.switchEnvironmentAndCloud('Environment', 'testcloud');
    common.sendValueToXEditable('p_numberBackup', '3', false);
    applications.switchEnvironmentAndCloud('ENV', 'testcloud');
    common.sendValueToXEditable('p_numberBackup', '6', false);
    applications.switchEnvironmentAndCloud('Environment', 'testcloud');
    applications.expectDeploymentPropertyValue('p_numberBackup', '3', true);
    applications.switchEnvironmentAndCloud('ENV', 'testcloud');
    applications.expectDeploymentPropertyValue('p_numberBackup', '6', true);
  });
});
