/**
* Cleanup is responsible of cleaning ElasticSearch repository and Alien4Cloud resources folders in order to make each test independant from the others.
*/

/* global protractor */

'use strict';

var settings = require('./settings');
var fs = require('fs');
var path = require('path');
var http = require('http');

var flow = protractor.promise.controlFlow();

function deleteFolderRecursive (folderPath, mustRemove) {
  if (fs.existsSync(folderPath)) {
    fs.readdirSync(folderPath).forEach(function(file) {
      var curPath = folderPath + path.sep + file;
      if (fs.statSync(curPath).isDirectory()) { // recurse
        deleteFolderRecursive(curPath, true);
      } else { // delete file
        fs.unlinkSync(curPath);
      }
    });
    if (mustRemove) {
      fs.rmdirSync(folderPath);
    }
  }
}

function cleanAlienRepository() {
  var alienBase = (process.env.HOME || process.env.HOMEPATH || process.env.USERPROFILE) + path.sep + '.alien' + path.sep;

  var alienArtifacts = alienBase + 'artifacts';
  deleteFolderRecursive(alienArtifacts, false);
  if (settings.debug) {
    console.log('################# Cleaned alien data files at base [' + alienBase + ']');
  }
}

function cleanElasticSearch(indexName) {
  var defer = protractor.promise.defer();
  var query = JSON.stringify({
    query: {
      match_all: {}
    }
  });

  // An object of options to indicate where to post to
  var deleteOptions = {
    host: 'localhost',
    port: '9200',
    path: '/' + indexName + '/_query',
    method: 'DELETE',
    headers: {
      'Content-Type': 'application/json; charset=UTF-8',
      'Content-Length': query.length
    }
  };

  // Set up the request
  var deleteRequest = http.request(deleteOptions, function(res) {
    res.setEncoding('utf8');
    res.on('data', function(chunk) {
      if (settings.debug) {
        console.log('################# Clean index [' + indexName + '] request received [' + chunk + ']');
      }
      defer.fulfill(res);
    });
  });

  deleteRequest.on('error', function(e) {
    console.log('################# Clean index [' + indexName + '] request encountered error [' + e.message + ']');
    defer.reject({
      error: e,
      message: '################# Clean index [' + indexName + '] request encountered error [' + e.message + ']'
    });
  });
  deleteRequest.write(query);
  deleteRequest.end();
  return defer.promise;
}

function cleanTagConfigurationElement() {
  return cleanElasticSearch('tagconfiguration');
}

function cleanToscaElement() {
  return cleanElasticSearch('toscaelement');
}

function cleanApplication() {
  return cleanElasticSearch('application');
}

function cleanTopology() {
  return cleanElasticSearch('topology');
}

function cleanImage() {
  return cleanElasticSearch('imagedata');
}

function cleanCsar() {
  return cleanElasticSearch('csar');
}

function cleanUsers() {
  return cleanElasticSearch('user');
}

function cleanTemplate() {
  return cleanElasticSearch('topologytemplate');
}

function cleanPlugins() {
  return cleanElasticSearch('plugin');
}

function cleanPluginConfigurations() {
  return cleanElasticSearch('pluginconfiguration');
}

function cleanClouds() {
  return cleanElasticSearch('cloud');
}

function cleanCloudConfigurations() {
  return cleanElasticSearch('cloudconfiguration');
}

function cleanDeployments() {
  return cleanElasticSearch('deployment');
}

function cleanGroups() {
  return cleanElasticSearch('group');
}

function cleanCloudImage() {
  return cleanElasticSearch('cloudimage');
}

function cleanup() {
  flow.execute(cleanApplication);
  flow.execute(cleanTopology);
  flow.execute(cleanUsers);
  flow.execute(cleanTemplate);
  flow.execute(cleanPluginConfigurations);
  flow.execute(cleanClouds);
  flow.execute(cleanCloudConfigurations);
  flow.execute(cleanDeployments);
  flow.execute(cleanTagConfigurationElement);
  flow.execute(cleanGroups);
  flow.execute(cleanCloudImage);
  cleanAlienRepository();
}
module.exports.cleanup = cleanup;

module.exports.fullCleanup = function() {
  flow.execute(cleanToscaElement);
  flow.execute(cleanCsar);
  flow.execute(cleanImage);
  flow.execute(cleanPlugins);
  cleanup();
  var alienBase = (process.env.HOME || process.env.HOMEPATH || process.env.USERPROFILE) + path.sep + '.alien' + path.sep;
  var alienRepository = alienBase + 'csar';
  deleteFolderRecursive(alienRepository, false);

  var alienPlugins = alienBase + 'plugins';
  deleteFolderRecursive(alienPlugins, false);
};
