define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-common').factory('userContextServices', function () {
    var previousEnvironmentsContext = {};
    var previousTopologiesContext = {};

    var getEnvironmentConntext = function (applicationId) {
      return previousEnvironmentsContext[applicationId];
    };

    var updateEnvironmentContext = function (applicationId, environment) {
      previousEnvironmentsContext[applicationId] = _.cloneDeep(environment);
    };

    var getTopologyContext = function (applicationId) {
      console.log('appId -> ' + _.get(previousTopologiesContext[applicationId], 'version.version') + '/' + _.get(previousTopologiesContext[applicationId], 'variant'));
      return previousTopologiesContext[applicationId];
    }

    var updateTopologyContext = function (applicationId, version, variant) {
      console.log('update -> ' + applicationId + ' ' + version.version + '/' + variant);

      previousTopologiesContext[applicationId] = {
        version: _.cloneDeep(version),
        variant: variant
      };
    };


    // should be called when a user logout
    var clear = function () {
      previousEnvironmentsContext = {};
      previousTopologiesContext = {};
    };

    return {
      'getEnvironmentConntext': getEnvironmentConntext,
      'updateEnvironmentContext': updateEnvironmentContext,
      'getTopologyContext': getTopologyContext,
      'updateTopologyContext': updateTopologyContext,
      'clear': clear
    };


  }); // factory
}); // define


/*
var userContextServices = function () {
  "use strict";



};
*/
