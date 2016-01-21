/* global element, by */

'use strict';

var common = require('../../common/common');
var componentData = require('../../topology/component_data');
var setup = require('../../common/setup');
var csars = require('../../components/csars');
var authentication = require('../../authentication/authentication');
var topologyEditorCommon = require('../../topology/topology_editor_common');
var applications = require('../../applications/applications');
var toaster = require('../../common/toaster');

describe('NodeTemplate relationships edition', function() {

  it('beforeAll', function() {
    setup.setup();
    common.home();
    authentication.login('applicationManager');
  });

  it('should be able to add an hostedOn and dependsOn relationship between a JAVA and COMPUTE nodetemplate', function() {
    console.log('################# should be able to add an hostedOn and dependsOn relationship between a JAVA and COMPUTE nodetemplate');
    applications.goToApplicationTopologyPage();
    topologyEditorCommon.addNodeTemplatesCenterAndZoom({
      compute2: componentData.simpleTopology.nodes.compute2,
      java: componentData.simpleTopology.nodes.java
    });

    topologyEditorCommon.addRelationship(componentData.simpleTopology.relationships.hostedOnCompute);
    topologyEditorCommon.addRelationship(componentData.simpleTopology.relationships.dependsOnCompute2);

    topologyEditorCommon.selectNodeAndGoToDetailBloc('Compute', topologyEditorCommon.nodeDetailsBlocsIds.rel);
    topologyEditorCommon.checkNumberOfRelationship(1);
    topologyEditorCommon.checkCreatedRelationship('dependsOnCompute2', 1);
    topologyEditorCommon.checkCreatedRelationship('myFakeRelationshipName', 0);

    topologyEditorCommon.selectNodeAndGoToDetailBloc('Java', topologyEditorCommon.nodeDetailsBlocsIds.rel);
    topologyEditorCommon.checkNumberOfRelationship(1);
    topologyEditorCommon.checkCreatedRelationship('hostedOnCompute', 1);
    topologyEditorCommon.checkCreatedRelationship('myFakeRelationshipName', 0);
  });


  it('should be able to remove a relationship', function() {
    console.log('################# should be able to remove a relationship');
    topologyEditorCommon.removeRelationship('hostedOnCompute');
    topologyEditorCommon.checkCreatedRelationship('hostedOnCompute', 0);
  });

  it('should not be able to add a relationship since no other node matches to the requirement', function() {
    console.log('################# should not be able to add a relationship since no other node matches to the requirement');

    topologyEditorCommon.addNodeTemplatesCenterAndZoom({war: componentData.alienTypes.war()});

    // i try to create a relationship with no target available in topology
    topologyEditorCommon.addRelationshipToNode('Java', 'War', 'host', 'tosca.relationships.HostedOn:' + componentData.normativeTypesVersion, 'hostedOnCompute');

    topologyEditorCommon.checkCreatedRelationship('hostedOnCompute', 0);
    topologyEditorCommon.checkCreatedRelationship('myFakeRelationshipName', 0);

    topologyEditorCommon.addRelationshipToNode('Java', 'Compute', 'host', 'tosca.relationships.HostedOn:' + componentData.normativeTypesVersion, 'hostedOnCompute');
    topologyEditorCommon.addNodeTemplatesCenterAndZoom({tomcat: componentData.alienTypes.tomcat()});
    topologyEditorCommon.addRelationshipToNode('Tomcat', 'Compute-2', 'host', 'tosca.relationships.HostedOn:' + componentData.normativeTypesVersion, 'hostedOnCompute');

    topologyEditorCommon.selectNodeAndGoToDetailBloc('War', topologyEditorCommon.nodeDetailsBlocsIds.rel);
    topologyEditorCommon.addRelationshipToNode('War', 'Tomcat', 'host', 'alien.relationships.WarHostedOnTomcat:' + componentData.tomcatTypesVersion, 'hostedOnTomcat');

    topologyEditorCommon.checkCreatedRelationship('hostedOnTomcat', 1);
    topologyEditorCommon.checkCreatedRelationship('myFakeRelationshipName', 0);
  });


  it('should not be able to add a relationship when upperBounds are reached in source or target', function() {
    console.log('################# should not be able to add a relationship when upperBounds are reached in source or target');
    expect(element(by.id(topologyEditorCommon.btnRelationshipNameBaseId + 'host')).isEnabled()).toBe(false);
  });

  it('afterAll', function() {
    authentication.logout();
  });
});
