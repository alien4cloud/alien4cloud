define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-common',['ngResource']).factory('featureService', [ '$resource',function($resource) {

    var features = $resource('rest/latest/configuration/supportedFeatures').get().$promise;

    return {
        autoValidation: function() {
            return features.then(function(result) {
                if ('autoValidation' in result.data) {
                    return result.data.autoValidation;
                } else {
                    return true;
                }
            });
        },
        lockNextDeploymentTabs: function() {
            return features.then(function(result) {
                if ('lockNextDeploymentTabs' in result.data) {
                    return result.data.lockNextDeploymentTabs;
                } else {
                    return true;
                }
            });
        }
    };
  }]); // factory
 });
