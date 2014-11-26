/* global element, by */

'use strict';

var common = require('../common/common');
var topologyEditorCommon = require('../topology/topology_editor_common');
var componentData = require('../topology/component_data');

describe('NodeTemplate relationships edition', function() {

  beforeEach(function() {
    topologyEditorCommon.beforeTopologyTest();
  });

  // After each spec in the tests suite(s)
  afterEach(function() {
    // Logout action
    common.after();
  });

  it('should be able to add an hostedOn and dependsOn relationship between a JAVA and COMPUTE nodetemplate', function() {
    console.log('################# should be able to add an hostedOn and dependsOn relationship between a JAVA and COMPUTE nodetemplate');

    topologyEditorCommon.addNodeTemplatesCenterAndZoom(componentData.simpleTopology.nodes);

    topologyEditorCommon.addRelationship(componentData.simpleTopology.relationships.hostedOnCompute);
    topologyEditorCommon.addRelationship(componentData.simpleTopology.relationships.dependsOnCompute2);

    browser.sleep(10000);

    // check on relationships count
    var relationships = element.all(by.repeater('(relationshipName,relationshipDefinition) in selectedNodeTemplate.relationships'));
    expect(relationships.count()).toBe(2);

    // Check created relationship
    topologyEditorCommon.checkCreatedRelationship('hostedOnCompute', 1);
    topologyEditorCommon.checkCreatedRelationship('dependsOnCompute2', 1);
    topologyEditorCommon.checkCreatedRelationship('myFakeRelationshipName', 0);
  });

  it('should not be able to add a relationship since no other node matches to the requirement', function() {
    console.log('################# should not be able to add a relationship since no other node matches to the requirement');

    topologyEditorCommon.addNodeTemplatesCenterAndZoom(componentData.simpleTopology.nodes);
    topologyEditorCommon.addNodeTemplatesCenterAndZoom({ war: componentData.fcTypes.war() });
    topologyEditorCommon.addRelationship(componentData.simpleTopology.relationships.dependsOnCompute2);
    var relationships = element.all(by.repeater('(relationshipName,relationshipDefinition) in selectedNodeTemplate.relationships'));
    expect(relationships.count()).toBe(1);

    // Check created relationship
    topologyEditorCommon.checkCreatedRelationship('dependsOnCompute2', 1);

    // i try to create a relationship with no target available in topology
    topologyEditorCommon.addRelationshipToNode('JavaRPM', 'War', 'host', 'tosca.relationships.HostedOn:2.0', 'hostedOnWar1');

    topologyEditorCommon.checkCreatedRelationship('hostedOnWar1', 0);
    topologyEditorCommon.checkCreatedRelationship('dependsOnCompute2', 1);
    topologyEditorCommon.checkCreatedRelationship('myFakeRelationshipName', 0);
  });

  it('should be able to remove a relationship', function() {
    console.log('################# should be able to remove a relationship');

    topologyEditorCommon.addNodeTemplatesCenterAndZoom(componentData.simpleTopology.nodes);

    topologyEditorCommon.addRelationship(componentData.simpleTopology.relationships.hostedOnCompute);
    topologyEditorCommon.addRelationship(componentData.simpleTopology.relationships.dependsOnCompute2);

    topologyEditorCommon.checkCreatedRelationship('hostedOnCompute', 1);
    topologyEditorCommon.checkCreatedRelationship('dependsOnCompute2', 1);

    topologyEditorCommon.removeRelationship('hostedOnCompute');
    topologyEditorCommon.checkCreatedRelationship('hostedOnCompute', 0);
  });

  it('should not be able to add a relationship when upperBounds are reached in source or target', function() {
    console.log('################# should not be able to add a relationship when upperBounds are reached in source or target');

    topologyEditorCommon.addNodeTemplatesCenterAndZoom(componentData.simpleTopology.nodes);

    topologyEditorCommon.addRelationship(componentData.simpleTopology.relationships.hostedOnCompute);
    // expect the button to be disabled
    topologyEditorCommon.checkCreatedRelationship('hostedOnCompute', 1);
    expect(element(by.id(topologyEditorCommon.btnRelationshipNameBaseId + 'host')).isEnabled()).toBe(false);
  });

});
