/* global by, element */
'use strict';

var authentication = require('../../authentication/authentication');
var common = require('../../common/common');
var tagConfigCommon = require('../../admin/metaprops_configuration_common');
var applications = require('../../applications/applications');
var cloudsCommon = require('../../admin/clouds_common');
var topologyEditorCommon = require('../../topology/topology_editor_common');
var navigation = require('../../common/navigation');
var componentData = require('../../topology/component_data');

describe('Application meta properties edition check', function() {

  /* Before each spec in the tests suite */
  beforeEach(function() {
    topologyEditorCommon.beforeTopologyTest();
  });

  /* After each spec in the tests suite(s) */
  afterEach(function() {
    // Logout action
    common.after();
  });

  it('should create an application an set configuration tags', function() {
    console.log('should create an application an set configuration tags');
    // add configuration tags
    authentication.reLogin('admin');
    tagConfigCommon.createConfigurationTags();

    // create the app
    authentication.reLogin('applicationManager');
    applications.createApplication('ALIEN_WITH_TAGS', 'Great application with configuration tags');

    // check p_name__ALIEN_RELEASE_VALID_VALUES class required
    var tagValidValuesName = element(by.id('p_name__ALIEN_RELEASE_VALID_VALUES'));
    expect(tagValidValuesName.getAttribute('class')).toContain('property-required');

    // suppose we've only one "select" in the configuration tag list
    var metaProperties = element(by.id('meta_properties'));
    var selectItem = metaProperties.element(by.tagName('select'));
    var selected = common.selectDropdownByText(selectItem, 'Q1');
    expect(selected).toBe(true); // Q1 is in the select

    // ---- NOT WORKING (strange bug)
    // enter a bad password text length
    // tagConfigCommon.editTagConfiguration('_ALIEN_PASSWORD_MIN4', 'bu');
    // browser.waitForAngular();

    // // check errors
    // tagConfigCommon.checkTagEditionError('_ALIEN_PASSWORD_MIN4', '4');
    // browser.waitForAngular();
    // ---- END : NOT WORKING (strange bug)

    // enter a good password text length
    tagConfigCommon.editTagConfiguration('_ALIEN_PASSWORD_MIN4', 'aaaa');
  });

  it('should add a cloud meta-property, set <Elementary> as value and use it in an application', function() {
    console.log('################# should add a cloud meta-property, set <Elementary> as value and use it in an application');
    // add a valid topology to the default application
    topologyEditorCommon.addNodeTemplatesCenterAndZoom(componentData.verySimpleTopology.nodes);
    topologyEditorCommon.editNodeProperty('Compute', 'os_arch', 'x86_64');
    topologyEditorCommon.editNodeProperty('Compute', 'os_type', 'linux');
    topologyEditorCommon.editNodeProperty('Compute', 'containee_types', 'test', 'cap');
    topologyEditorCommon.togglePropertyInput('Compute', 'os_distribution');

    // add an empty cloud meta property
    authentication.reLogin('admin');
    cloudsCommon.goToCloudList();
    cloudsCommon.goToCloudDetail('testcloud');
    tagConfigCommon.addTagConfiguration(tagConfigCommon.defaultCloudProperty, null);

    // the app should don't have a todo list
    applications.goToApplicationDetailPage('Alien', false);
    navigation.go('main', 'applications');
    browser.element(by.binding('application.name')).click();
    navigation.go('applications', 'deployment');
    cloudsCommon.selectApplicationCloud('testcloud');
    topologyEditorCommon.checkTodoList(false);

    // now we map the input to the cloud meta-property, the todo list should be display because the meta-property is empty
    navigation.go('applications', 'topology');
    topologyEditorCommon.renameApplicationInput('os_distribution', 'cloud_meta_distribution', false);
    navigation.go('applications', 'deployment');
    topologyEditorCommon.checkTodoList(false);
    topologyEditorCommon.checkWarningList(true);

    // now we set the value of the cloud meta-property, should be remove the todo list
    cloudsCommon.goToCloudList();
    cloudsCommon.goToCloudDetail('testcloud');
    cloudsCommon.goToCloudConfiguration();
    cloudsCommon.showMetaProperties();
    tagConfigCommon.editTagConfiguration('distribution', 'Elementary');
    applications.goToApplicationDetailPage('Alien', false);
    navigation.go('main', 'applications');
    browser.element(by.binding('application.name')).click();
    navigation.go('applications', 'deployment');
    topologyEditorCommon.checkTodoList(false);
  });
});
