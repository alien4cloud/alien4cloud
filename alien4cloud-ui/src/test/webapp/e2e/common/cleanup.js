/**
 * Cleanup is responsible of cleaning ElasticSearch repository and Alien4Cloud resources folders in order to make each test independant from the others.
 */

/* global protractor */

'use strict';

var es = require('./elasticsearch');
var repositories = require('./repositories');

var flow = protractor.promise.controlFlow();

function cleanUser() {
  return es.delete('user');
}

function cleanGroups() {
  // Do not clean all users group
  return es.delete('group', JSON.stringify({
    'query': {
      'bool': {
        'must_not': {
          'term': {
            'name': 'ALL_USERS'
          }
        }
      }
    }
  }));
}

function cleanPluginConfiguration() {
  return es.delete('pluginconfiguration');
}

function cleanOrchestrator() {
  return es.delete('orchestrator');
}

function cleanOrchestratorConfiguration() {
  return es.delete('orchestratorconfiguration');
}

function cleanLocation() {
  return es.delete('location');
}

function cleanLocationResourceTemplate() {
  return es.delete('locationresourcetemplate');
}

function cleanMetaPropConfiguration() {
  return es.delete('metapropconfiguration');
}

function cleanTopologyTemplate() {
  return es.delete('topologytemplate');
}

function cleanApplication() {
  return es.delete('application');
}

function cleanApplicationEnvironment() {
  return es.delete('applicationenvironment');
}

function cleanApplicationVersion() {
  return es.delete('applicationversion');
}

function cleanTopology() {
  return es.delete('topology');
}

function cleanDeployment() {
  return es.delete('deployment');
}

function cleanToscaElement() {
  return es.delete('toscaelement');
}

function cleanCsar() {
  return es.delete('csar');
}

function cleanImagedata() {
  return es.delete('imagedata');
}

function cleanPlugin() {
  return es.delete('plugin');
}

function cleanup() {
  flow.execute(cleanUser);
  flow.execute(cleanGroups);
  flow.execute(cleanPluginConfiguration);
  flow.execute(cleanMetaPropConfiguration);
  flow.execute(cleanTopologyTemplate);
  flow.execute(cleanApplication);
  flow.execute(cleanApplicationEnvironment);
  flow.execute(cleanApplicationVersion);
  flow.execute(cleanTopology);
  flow.execute(cleanDeployment);
  flow.execute(cleanOrchestrator);
  flow.execute(cleanOrchestratorConfiguration);
  flow.execute(cleanLocation);
  flow.execute(cleanLocationResourceTemplate);
  repositories.rmArtifacts();
}
module.exports.cleanup = cleanup;

module.exports.fullCleanup = function() {
  flow.execute(cleanToscaElement);
  flow.execute(cleanCsar);
  flow.execute(cleanImagedata);
  flow.execute(cleanPlugin);
  cleanup();
  repositories.rmArchives();
  repositories.rmPlugins();
};
