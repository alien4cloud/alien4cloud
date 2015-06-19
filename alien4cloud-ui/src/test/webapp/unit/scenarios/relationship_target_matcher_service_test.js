'use strict';

describe('Relationship target matching tests', function() {

  var typesMap, service;

  beforeEach(angular.mock.module('alienUiApp'));

  beforeEach(function() {
    jasmine.getJSONFixtures().fixturesPath = 'base/test/mock';
    typesMap = getJSONFixture('relationship_matching_types.json');
  });

  beforeEach(angular.mock.inject(function($injector){
    service = $injector.get('relationshipTopologyService');
  }));

  it('Should match a valid node type correctly against a single target', function() {
    var isValid = service.getNodeTarget([['alien4cloud.test.nodes.Software']], {type: 'alien4cloud.test.nodes.Software'}, typesMap.nodeTypes) !== null;
    expect(isValid).toBeTruthy();
  });

  it('Should not match an invalid node type correctly against a single target', function() {
    var isValid = service.getNodeTarget([['alien4cloud.test.nodes.Softwaree']], {type: 'alien4cloud.test.nodes.Software'}, typesMap.nodeTypes) !== null;
    expect(isValid).toBeFalsy();
  });

  it('Should match a valid node derived type correctly against a single target', function() {
    var isValid = service.getNodeTarget([['tosca.nodes.SoftwareComponent']], {type: 'alien4cloud.test.nodes.Software'}, typesMap.nodeTypes) !== null;
    expect(isValid).toBeTruthy();
  });

  it('Should not match a in valid node derived type correctly against a single target', function() {
    var isValid = service.getNodeTarget(['tosca.nodes.SoftwareComponentt'], {type: 'alien4cloud.test.nodes.Software'}, typesMap.nodeTypes) !== null;
    expect(isValid).toBeFalsy();
  });

  it('Should match a valid capability type correctly against a single target', function() {
    var isValid = service.getCapabilityTarget([['tosca.capabilities.Container']],
      {
        type: 'alien4cloud.test.nodes.Software',
        capabilities: { host: {canAddRel: {yes: true} } }
      },
      typesMap.nodeTypes, typesMap.capabilityTypes) !== null;
    expect(isValid).toBeTruthy();
  });

  it('Should match a valid derived capability type correctly against a single target', function() {
    var isValid = service.getCapabilityTarget([['tosca.capabilities.Root']],
      {
        type: 'alien4cloud.test.nodes.Software',
        capabilities: { host: {canAddRel: {yes: true} } }
      },
      typesMap.nodeTypes, typesMap.capabilityTypes) !== null;
    expect(isValid).toBeTruthy();
  });
});
