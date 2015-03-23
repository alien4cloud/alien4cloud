/* global element, by */

'use strict';

var common = require('../../common/common');
var navigation = require('../../common/navigation');
var topologyEditorCommon = require('../../topology/topology_editor_common');
var componentData = require('../../topology/component_data');
var cloudsCommon = require('../../admin/clouds_common');

describe('NodeTemplate relationships edition', function() {

  beforeEach(function() {
    topologyEditorCommon.beforeTopologyTest();
    topologyEditorCommon.addNodeTemplatesCenterAndZoom(componentData.simpleTopology.nodes);
  });

  // After each spec in the tests suite(s)
  afterEach(function() {
    // Logout action
    common.after();
  });

  var checkInputProperty = function(inputId) {
    expect(element(by.id('app_deployment_id_' + inputId)).isPresent()).toBe(true);
  };

  it('should be able display the capability properties', function() {
    console.log('################# should be able display the capability properties');
    var nodeToEdit = element(by.id('rect_Compute'));
    nodeToEdit.click();
    topologyEditorCommon.checkNumberOfPropertiesForACapability(1);

    nodeToEdit = element(by.id('rect_JavaRPM'));
    nodeToEdit.click();
    topologyEditorCommon.checkNumberOfPropertiesForACapability(5);
  });

  it('should be able to define a capability property as input', function() {
    console.log('################# should be able to define a capability property as input');
    var nodeToEdit = element(by.id('rect_JavaRPM'));
    nodeToEdit.click();

    topologyEditorCommon.checkCountInputs(0);
    topologyEditorCommon.togglePropertyInput('JavaRPM', 'update', 'cap');
    topologyEditorCommon.checkCountInputs(1);
    topologyEditorCommon.togglePropertyInput('JavaRPM', 'vendor', 'cap');
    topologyEditorCommon.checkCountInputs(2);
  });

  it('should be able associate a capability property to an already existing input', function() {
    console.log('################# should be able associate a capability property to an already existing input.');
    navigation.go('applications', 'deployment');
    cloudsCommon.selectApplicationCloud('testcloud');

    navigation.go('applications', 'topology');
    topologyEditorCommon.togglePropertyInput('Compute', 'containee_types', 'cap');
    topologyEditorCommon.associatePropertyToInput('Compute_2', 'containee_types', 'containee_types', 'cap');
    topologyEditorCommon.checkCountInputs(1);

    topologyEditorCommon.editNodeProperty('Compute', 'os_arch', 'x86_64');
    topologyEditorCommon.editNodeProperty('Compute', 'os_type', 'windows');
    topologyEditorCommon.editNodeProperty('Compute_2', 'os_arch', 'x86_64');
    topologyEditorCommon.editNodeProperty('Compute_2', 'os_type', 'windows');
    topologyEditorCommon.addRelationshipToNode('JavaRPM', 'Compute', 'host', 'tosca.relationships.HostedOn:2.0', 'hostedOnComputeHost');

    topologyEditorCommon.expectShowTodoList(true, true);
    checkInputProperty('containee_types');
    common.sendValueToXEditable('p_', 'test', false);
    topologyEditorCommon.expectShowTodoList(false, false);
  });
});
