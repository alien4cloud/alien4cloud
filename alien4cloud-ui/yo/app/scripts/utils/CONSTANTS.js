'use strict';

var CONSTANTS = {};

// topolopgy designer concerns
CONSTANTS.toscaComputeType = 'tosca.nodes.Compute';
CONSTANTS.toscaStandardInterfaceName = 'tosca.interfaces.node.lifecycle.Standard';
CONSTANTS.cloudify2extensionInterfaceName = 'fastconnect.cloudify.extensions';
CONSTANTS.minimumZoneCountPerGroup = 1;

// internal inputs
CONSTANTS.excludedInputs = ['cloud_metas_', 'cloud_tags_', 'app_metas_', 'app_tags_', 'env_metas_', 'env_tags_'];
