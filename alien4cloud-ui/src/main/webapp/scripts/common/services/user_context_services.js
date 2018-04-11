define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

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
      return previousTopologiesContext[applicationId];
    };

    var deleteTopologyContext = function (applicationId) {
      delete previousTopologiesContext[applicationId];
    };

    var updateTopologyContext = function (applicationId, version, variant) {
      previousTopologiesContext[applicationId] = {
        version: version.id,
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
      'deleteTopologyContext': deleteTopologyContext,
      'clear': clear
    };


  }); // factory
}); // define
