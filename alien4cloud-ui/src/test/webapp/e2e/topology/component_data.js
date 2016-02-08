// Contains components objects available for topology tests.
'use strict';

function instanceId(node, instance) {
  node.id = node.id + '-' + instance;
  return node;
}

var normativeTypesVersion = '1.0.0.wd06-SNAPSHOT';
module.exports.normativeTypesVersion = normativeTypesVersion;

var tomcatTypesVersion = '2.0.0-SNAPSHOT';
module.exports.tomcatTypesVersion = tomcatTypesVersion;

var toscaBaseTypes = {
  get: function(node, selectedVersion) {
    node.archiveVersion = normativeTypesVersion;
    node.selectedVersion = selectedVersion;
    return node;
  },
  compute: function(selectedVersion) {
    return this.get({
      type: 'tosca.nodes.Compute',
      id: 'rect_Compute'
    }, selectedVersion);
  },
  network: function(selectedVersion) {
    return this.get({
      type: 'tosca.nodes.Network',
      id: 'rect_Network'
    }, selectedVersion);
  },
  blockstorage: function(selectedVersion) {
    return this.get({
      type: 'tosca.nodes.BlockStorage',
      id: 'rect_BlockStorage'
    }, selectedVersion);
  }
};
module.exports.toscaBaseTypes = toscaBaseTypes;

var tomcatTypes = {
  get: function(node, selectedVersion) {
    node.archiveVersion = tomcatTypesVersion;
    node.selectedVersion = selectedVersion;
    return node;
  },
  java: function(requestedVersion) {
    return this.get({
      type: 'alien.nodes.Java',
      id: 'rect_Java'
    }, requestedVersion);
  },
  tomcat: function(requestedVersion) {
    return this.get({
      type: 'alien.nodes.Tomcat',
      id: 'rect_Tomcat'
    }, requestedVersion);
  },
  war: function(requestedVersion) {
    return this.get({
      type: 'alien.nodes.War',
      id: 'rect_War'
    }, requestedVersion);
  }
};
module.exports.alienTypes = tomcatTypes;

module.exports.simpleTopology = {
  nodes: {
    compute: toscaBaseTypes.compute(),
    compute2: instanceId(toscaBaseTypes.compute(), 2),
    java: tomcatTypes.java()
  },
  relationships: {
    hostedOnCompute: {
      name: 'hostedOnCompute',
      source: 'Java',
      requirement: 'host',
      target: 'Compute',
      type: 'tosca.relationships.HostedOn:' + normativeTypesVersion
    },
    dependsOnCompute2: {
      name: 'dependsOnCompute2',
      source: 'Compute',
      requirement: 'dependency',
      target: 'Compute-2',
      type: 'tosca.relationships.DependsOn:' + normativeTypesVersion
    }
  }
};