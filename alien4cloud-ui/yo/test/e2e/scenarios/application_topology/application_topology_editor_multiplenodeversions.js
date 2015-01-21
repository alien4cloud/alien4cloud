/* global element, by */
'use strict';

var common = require('../common/common');
var topologyEditorCommon = require('../topology/topology_editor_common');
var componentData = require('../topology/component_data');

describe('Topology with multiple node versions', function() {

  beforeEach(function() {
    topologyEditorCommon.beforeTopologyTest();
  });

  afterEach(function() {
    common.after();
  });

  it('should be able construct a topology by choosing between different versions', function() {
    console.log('################# should be able construct a topology by choosing between different versions');
    topologyEditorCommon.addNodeTemplatesCenterAndZoom({
        java: componentData.fcTypes.java(),
        compute: componentData.toscaBaseTypes.compute('1.0')
      });
    // topologyEditorCommon.addRelationshipToNode('Java', 'Compute', 'host', 'tosca.relationships.HostedOn:2.0', 'hostedOnCompute', undefined, '1.0', 'tosca.relationships.HostedOn:1.0');
    // topologyEditorCommon.checkCreatedRelationship('hostedOnCompute', 1);
    // element(by.id('topology-dependencies')).click();
    // element.all(by.repeater('dependency in topology.topology.dependencies')).then(function(dependenciesElements) {
    //   expect(dependenciesElements.length).toEqual(2);
    //   for (var i = 0; i < dependenciesElements.length; i++) {
    //     // checkDependencies(dependenciesElements[i]);
    //   }
    // });
  });
});
