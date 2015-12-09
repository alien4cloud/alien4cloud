// Setup is used to pre-configure data for the ui tests. It should be called as a before all for each test suite.

/* global protractor */

'use strict';

var cleanup = require('./cleanup');
var repositories = require('./repositories');
var settings = require('./settings');
var path = require('path');
var es = require('./elasticsearch');
var alien = require('./alienapi');
var flow = protractor.promise.controlFlow();

// constants to be used for the setup.
var csargitrepositories = require(__dirname + '/../_data/csargitrepositories.json');
var csars = require(__dirname + '/../_data/csars.json');
var indexedartifacttypes = require(__dirname + '/../_data/indexedartifacttypes.json');
var indexedcapabilitytypes = require(__dirname + '/../_data/indexedcapabilitytypes.json');
var indexeddatatypes = require(__dirname + '/../_data/indexeddatatypes.json');
var indexednodetypes = require(__dirname + '/../_data/indexednodetypes.json');
var indexedrelationshiptypes = require(__dirname + '/../_data/indexedrelationshiptypes.json');
var imagedatas = require(__dirname + '/../_data/imagedatas.json');

var topologytemplates = require(__dirname + '/../_data/topologytemplates.json');
var topologytemplateversions = require(__dirname + '/../_data/topologytemplateversions.json');
var topologies = require(__dirname + '/../_data/topologies.json');

var plugins = require(__dirname + '/../_data/plugins.json');
var orchestrators = require(__dirname + '/../_data/orchestrators.json');
var orchestratorsconf = require(__dirname + '/../_data/orchestratorconfiguration.json');
var locations = require(__dirname + '/../_data/locations.json');
var locationresourcetemplates = require(__dirname + '/../_data/locationresourcetemplates.json');

var applications = require(__dirname + '/../_data/applications.json');
// archives folders to copy
var toscaNormativeTypes = path.resolve(__dirname, '../../../../../../alien4cloud-rest-it/target/git/tosca-normative-types-wd06');
var imagesPath = path.resolve(__dirname + '/../_data/images');

// plugins to copy
var mockPlugin10Path = path.resolve(__dirname, '../../../../../../alien4cloud-mock-paas-provider-plugin/target/alien4cloud-mock-paas-provider-plugin-1.0-' + settings.version.version);
var mockPluginArchive = path.resolve(__dirname, '../../../../../../alien4cloud-mock-paas-provider-plugin/src/main/resources/openstack/mock-resources');
var mockPluginOSArchive = path.resolve(__dirname, '../../../../../../alien4cloud-mock-paas-provider-plugin/src/main/resources/openstack/mock-openstack-resources');

function index(indexName, typeName, data) {
  for(var i=0; i < data.length ;i++) {
    es.index(indexName, typeName, JSON.stringify(data[i]));
  }
}

function doSetup() {
  // Copy files for plugins and archives
  repositories.copyPlugin('eab8c56e-3d94-4e69-8c3f-1dafdc30d5e0', mockPlugin10Path);
  repositories.copyArchive('mock-plugin-types', '1.1.0-SM6-SNAPSHOT', mockPluginArchive);
  repositories.copyArchive('mock-plugin-openstack-types', '1.1.0-SM6-SNAPSHOT', mockPluginOSArchive);
  repositories.copyArchive('tosca-normative-types', '1.0.0.wd06-SNAPSHOT', toscaNormativeTypes);
  repositories.copyImages(imagesPath);

  // Update ElasticSearch
  index('csargitrepository', 'csargitrepository', csargitrepositories);
  index('csar', 'csar', csars);
  index('toscaelement', 'indexedartifacttype', indexedartifacttypes);
  index('toscaelement', 'indexedcapabilitytype', indexedcapabilitytypes);
  index('toscaelement', 'indexeddatatype', indexeddatatypes);
  index('toscaelement', 'indexednodetype', indexednodetypes);
  index('toscaelement', 'indexedrelationshiptype', indexedrelationshiptypes);
  index('imagedata', 'imagedata', imagedatas);

  index('topologytemplates', 'topologytemplates', imagedatas);
  index('topologytemplateversions', 'topologytemplateversions', imagedatas);
  index('topologies', 'topologies', imagedatas);

  index('plugin', 'plugin', plugins);
  index('orchestrator', 'orchestrator', orchestrators);
  index('orchestratorconfiguration', 'orchestratorconfiguration', orchestratorsconf);
  index('location', 'location', locations);
  index('locationresourcetemplate', 'locationresourcetemplate', locationresourcetemplates);

  index('application', 'application', applications);

  // 'Cookie': 'JSESSIONID = ',
  alien.login('admin', 'admin').then(function(response){
    var cookies = response.headers['set-cookie'];
    // Enable the plugins - this has to be done through alien API as it has to load classes.
    alien.enablePlugin('alien4cloud-mock-paas-provider:1.0', cookies);
    // Enable the orchestrators - this has to be done through alien API as it has to register the orchestrator monitor.
    alien.enableOrchestrator('f3657e4d-4250-45b4-a862-2e91699ef7a1', cookies);
    alien.enableOrchestrator('91c78b3e-e9fa-4cda-80ba-b44551e4a475', cookies);
  });
}

module.exports.setup = function() {
  cleanup.fullCleanup();
  flow.execute(doSetup);
};
