/* global element, by */

'use strict';

var common = require('../../common/common');
var navigation = require('../../common/navigation');
var topologyEditorCommon = require('../../topology/topology_editor_common');
var componentData = require('../../topology/component_data');
var cloudsCommon = require('../../admin/clouds_common');

describe('NodeTemplate relationships/capability edition', function() {

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
    topologyEditorCommon.checkNumberOfPropertiesForACapability('scalable', 3);

    nodeToEdit = element(by.id('rect_JavaRPM'));
    nodeToEdit.click();
    topologyEditorCommon.checkNumberOfPropertiesForACapability('java', 5);
  });

});
