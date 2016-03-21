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
var suggestionentry = require(__dirname + '/../_data/suggestionentry.json');
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
var applicationenvironments = require(__dirname + '/../_data/applicationenvironments.json');
var applicationversions = require(__dirname + '/../_data/applicationversions.json');

// archives folders to copy
var toscaNormativeTypes = path.resolve(__dirname, '../../../../../../alien4cloud-rest-it/target/git/tosca-normative-types-wd06');
var imagesPath = path.resolve(__dirname + '/../_data/images');

// plugins to copy
var mockPlugin10Path = path.resolve(__dirname, '../../../../../../alien4cloud-mock-paas-provider-plugin/target/alien4cloud-mock-paas-provider-plugin-1.0-' + settings.version.version);
var mockPluginArchive = path.resolve(__dirname, '../../../../../../alien4cloud-mock-paas-provider-plugin/src/main/resources/openstack/mock-resources');
var mockPluginOSArchive = path.resolve(__dirname, '../../../../../../alien4cloud-mock-paas-provider-plugin/src/main/resources/openstack/mock-openstack-resources');

// user and group
var users = require(__dirname + '/../_data/users.json');
var groups = require(__dirname + '/../_data/groups.json');

function index(indexName, typeName, data) {
  var defer = protractor.promise.defer();
  var promises = [];
  for (var i = 0; i < data.length; i++) {
    var indexPromise = es.index(indexName, typeName, JSON.stringify(data[i]));
    promises.push(indexPromise);
  }
  protractor.promise.all(promises).then(function() {
    es.refresh(indexName).then(function() {
      defer.fulfill('done');
    });
  });
  return defer.promise;
}

function doSetup() {
  // Copy files for plugins and archives
  repositories.copyPlugin('eab8c56e-3d94-4e69-8c3f-1dafdc30d5e0', mockPlugin10Path);
  repositories.copyArchive('mock-plugin-types', '1.1.0-SM6-SNAPSHOT', mockPluginArchive);
  repositories.copyArchive('mock-plugin-openstack-types', '1.1.0-SM6-SNAPSHOT', mockPluginOSArchive);
  repositories.copyArchive('tosca-normative-types', '1.0.0.wd06-SNAPSHOT', toscaNormativeTypes);
  repositories.copyImages(imagesPath);

  // Update ElasticSearch
  flow.execute(function() {
    return index('csargitrepository', 'csargitrepository', csargitrepositories);
  });
  flow.execute(function() {
    return index('csar', 'csar', csars);
  });
  flow.execute(function() {
    return index('toscaelement', 'indexedartifacttype', indexedartifacttypes);
  });
  flow.execute(function() {
    return index('toscaelement', 'indexedcapabilitytype', indexedcapabilitytypes);
  });
  flow.execute(function() {
    return index('toscaelement', 'indexeddatatype', indexeddatatypes);
  });
  flow.execute(function() {
    return index('toscaelement', 'indexednodetype', indexednodetypes);
  });
  flow.execute(function() {
    return index('toscaelement', 'indexedrelationshiptype', indexedrelationshiptypes);
  });
  flow.execute(function() {
    return index('suggestionentry', 'suggestionentry', suggestionentry);
  });

  flow.execute(function() {
    return index('topologytemplate', 'topologytemplate', topologytemplates);
  });
  flow.execute(function() {
    return index('topologytemplateversion', 'topologytemplateversion', topologytemplateversions);
  });
  flow.execute(function() {
    return index('topology', 'topology', topologies);
  });

  flow.execute(function() {
    return index('plugin', 'plugin', plugins);
  });
  flow.execute(function() {
    return index('orchestrator', 'orchestrator', orchestrators);
  });
  flow.execute(function() {
    return index('orchestratorconfiguration', 'orchestratorconfiguration', orchestratorsconf);
  });
  flow.execute(function() {
    return index('location', 'location', locations);
  });
  flow.execute(function() {
    return index('locationresourcetemplate', 'locationresourcetemplate', locationresourcetemplates);
  });

  flow.execute(function() {
    return index('application', 'application', applications);
  });
  flow.execute(function() {
    return index('applicationenvironment', 'applicationenvironment', applicationenvironments);
  });
  flow.execute(function() {
    return index('applicationversion', 'applicationversion', applicationversions);
  });

  flow.execute(function() {
    return index('user', 'user', users);
  });
  flow.execute(function() {
    return index('group', 'group', groups);
  });

  flow.execute(function() {
    return index('imagedata', 'imagedata', imagedatas);
  });

}

function doInitializePlatform() {
  var defer = protractor.promise.defer();
  alien.login('admin', 'admin').then(function(response) {
    var cookies = response.headers['set-cookie'];
    // Enable the plugins - this has to be done through alien API as it has to load classes.
    alien.teardownPlatform(cookies).then(function() {
      alien.initPlatform(cookies).then(function() {
        defer.fulfill('done');
      });
    })
  });
  return defer.promise;
}

module.exports.setup = function() {
  // This setup test data
  cleanup.fullCleanup();
  doSetup();
  flow.execute(doInitializePlatform);
};

module.exports.index = function(indexName, typeName, data) {
  flow.execute(function() {
    return index(indexName, typeName, data);
  });
};
