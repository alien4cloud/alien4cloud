// Setup is used to pre-configure data for the ui tests. It should be called as a before all for each test suite.

/* global protractor */

'use strict';

var cleanup = require('./cleanup');
var repositories = require('./repositories');
var settings = require('./settings');
var path = require('path');
var es = require('./elasticsearch');
var flow = protractor.promise.controlFlow();

// constants to be used for the setup.
var csargitrepositories = require(__dirname + '/../_data/csargitrepositories.json');
var csars = require(__dirname + '/../_data/csars.json');
var toscaelements = require(__dirname + '/../_data/toscaelements.json');

var plugins = require(__dirname + '/../_data/plugins.json');
var orchestrators = require(__dirname + '/../_data/orchestrators.json');
var orchestratorsconf = require(__dirname + '/../_data/orchestratorconfiguration.json');
var locations = require(__dirname + '/../_data/locations.json');
var locationresourcetemplates = require(__dirname + '/../_data/locationresourcetemplates.json');

var applications = require(__dirname + '/../_data/applications.json');
// archives folders to copy
var toscaNormativeTypes = path.resolve(__dirname, '../../../../../../alien4cloud-rest-it/target/git/tosca-normative-types-wd06');

// plugins to copy
var mockPlugin10Path = path.resolve(__dirname, '../../../../../../alien4cloud-mock-paas-provider-plugin/target/alien4cloud-mock-paas-provider-plugin-1.0-' + settings.version.version);
var mockPluginArchive = path.resolve(__dirname, '../../../../../../alien4cloud-mock-paas-provider-plugin/src/main/resources/openstack/mock-resources');
var mockPluginOSArchive = path.resolve(__dirname, '../../../../../../alien4cloud-mock-paas-provider-plugin/src/main/resources/openstack/mock-openstack-resources');

function index(indexName, typeName, data) {
  // var batch = null;
  for(var i=0; i < data.length ;i++) {
    // if(batch === null) {
    //   batch = '{ "index": { "_index": "'+indexName+'", "_type": "'+typeName+'", "_id": "' + element[idField] + '" }}\n';
    //   batch += element
    // } else {
    //
    // }
    es.index(indexName, typeName, JSON.stringify(data[i]));
  }
}

function doSetup() {
  // Copy files for plugins and archives
  repositories.copyPlugin('eab8c56e-3d94-4e69-8c3f-1dafdc30d5e0', mockPlugin10Path);
  repositories.copyArchive('mock-plugin-types', '1.1.0-SM6-SNAPSHOT', mockPluginArchive);
  repositories.copyArchive('mock-plugin-openstack-types', '1.1.0-SM6-SNAPSHOT', mockPluginOSArchive);
  repositories.copyArchive('tosca-normative-types', '1.0.0.wd06-SNAPSHOT', toscaNormativeTypes);

  // Update ElasticSearch
  index('csargitrepository', 'csargitrepository', csargitrepositories);
  index('csar', 'csar', csars);
  index('toscaelement', 'toscaelement', toscaelements);
  index('plugin', 'plugin', plugins);
  index('orchestrator', 'orchestrator', orchestrators);
  index('orchestratorconfiguration', 'orchestratorconfiguration', orchestratorsconf);
  index('location', 'location', locations);
  index('locationresourcetemplate', 'locationresourcetemplate', locationresourcetemplates);

  index('application', 'application', applications);
}

module.exports.setup = function() {
  cleanup.fullCleanup();
  flow.execute(doSetup);
};
