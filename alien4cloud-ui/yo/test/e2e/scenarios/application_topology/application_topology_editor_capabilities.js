/* global element, by */

'use strict';

var common = require('../../common/common');
var topologyEditorCommon = require('../../topology/topology_editor_common');
var componentData = require('../../topology/component_data');

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
    var nodeToEdit = element(by.id('rect_Compute'));
    nodeToEdit.click();

    topologyEditorCommon.togglePropertyInput('Compute', 'containee_types', 'cap');
    topologyEditorCommon.associatePropertyToInput('Compute_2', 'containee_types', 'containee_types', 'cap');
    topologyEditorCommon.checkCountInputs(1);
  });
});
