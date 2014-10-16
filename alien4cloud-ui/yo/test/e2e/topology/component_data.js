// Contains components objects available for topology tests.
'use strict';

function instanceId(node, instance) {
  node.id = node.id + '_' + instance;
  return node;
}

var toscaBaseTypes = {
  get: function(node, selectedVersion) {
    node.archiveVersion = '2.0';
    node.selectedVersion = selectedVersion;
    return node;
  },
  compute: function(selectedVersion) {
    return this.get({
        type: 'tosca.nodes.Compute',
        id: 'rect_Compute'
      }, selectedVersion);
  }
};
module.exports.toscaBaseTypes = toscaBaseTypes;

var fcTypes = {
  get: function(node, selectedVersion) {
    node.archiveVersion = '2.0';
    node.selectedVersion = selectedVersion;
    return node;
  },
  java: function(requestedVersion) {
    return this.get({
        type: 'fastconnect.nodes.Java',
        id: 'rect_Java',
      }, requestedVersion);
  },
  javaRPM: function(requestedVersion) {
    return this.get({
        type: 'fastconnect.nodes.JavaRPM',
        id: 'rect_JavaRPM',
      }, requestedVersion);
  },
  war: function(requestedVersion) {
    return this.get({
        type: 'fastconnect.nodes.War',
        id: 'rect_War',
      }, requestedVersion);
  },
  tomcatRpm: function(requestedVersion) {
    return this.get({
        type: 'fastconnect.nodes.TomcatRPM',
        id: 'rect_TomcatRPM',
      }, requestedVersion);
  }
};
module.exports.fcTypes = fcTypes;

var apacheTypes = {
  get: function(node, selectedVersion) {
    node.archiveVersion = '0.2';
    node.selectedVersion = selectedVersion;
    return node;
  },
  apacheLBGroovy: function(requestedVersion) {
    return this.get({
        type: 'fastconnect.nodes.apacheLBGroovy',
        id: 'rect_apacheLBGroovy',
      }, requestedVersion);
  }
};
module.exports.apacheTypes = apacheTypes;

module.exports.simpleTopology = {
  nodes: {
    compute: toscaBaseTypes.compute(),
    compute2: instanceId(toscaBaseTypes.compute(), 2),
    java: fcTypes.javaRPM()
  },
  relationships: {
    hostedOnCompute: {
      name: 'hostedOnCompute',
      source: 'JavaRPM',
      requirement: 'host',
      target: 'Compute',
      type: 'tosca.relationships.HostedOn:2.0'
    },
    dependsOnCompute2: {
      name: 'dependsOnCompute2',
      source: 'JavaRPM',
      requirement: 'dependency',
      target: 'Compute_2',
      type: 'tosca.relationships.DependsOn:2.0'
    }
  }
};

module.exports.verySimpleTopology = {
  nodes: {
    compute: toscaBaseTypes.compute()
  }
};

module.exports.simpleAbstractTopology = {
  nodes: {
    compute: toscaBaseTypes.compute(),
    compute2: instanceId(toscaBaseTypes.compute(), 2),
    java: fcTypes.java()
  },
  relationships: {
    hostedOnCompute: {
      name: 'hostedOnCompute',
      source: 'Java',
      requirement: 'host',
      target: 'Compute',
      type: 'tosca.relationships.HostedOn:2.0'
    },
    dependsOnCompute2: {
      name: 'dependsOnCompute2',
      source: 'Java',
      requirement: 'dependency',
      target: 'Compute_2',
      type: 'tosca.relationships.DependsOn:2.0'
    }
  }
};
