define(function (require) {
  'use strict';

  var modules = require('modules');
  var angular = require('angular');
  var _ = require('lodash');

  modules.get('a4c-metas', ['ngResource']).factory('metapropConfServices', ['$resource',
    function($resource) {
      var MetapropConfigurationDAO = $resource('rest/tagconfigurations/:id');

      var searchMetapropConfiguration = $resource('rest/tagconfigurations/search', {}, {
        'search': {
          method: 'POST',
          isArray: false,
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          }
        }
      });

      var get = function(id) {
        return MetapropConfigurationDAO.get({
          'id': id
        }).$promise.then(function(result) {
            return result.data;
          });
      };

      var remove = function(id) {
        return MetapropConfigurationDAO.remove({
          'id': id
        }).$promise.then(function(result) {
            return result.data;
          });
      };

      var save = function(config) {
        return MetapropConfigurationDAO.save({}, angular.toJson(config)).$promise.then(function(result) {
          return result.data;
        });
      };

      var processValidationErrors = function(validationErrors) {
        if (validationErrors) {
          var errors = {};
          for (var i = 0; i < validationErrors.length; i++) {
            var error = validationErrors[i];
            if (_.defined(errors[error.error]) && !_.isEmpty(errors[error.error])) {
              errors[error.error].push(error.path);
            } else {
              // TODO manage correctly server validation
              errors[error.error] = [
                [error.path]
              ];
            }
          }
          return errors;
        }
      };

      return {
        'search': searchMetapropConfiguration.search,
        'get': get,
        'remove': remove,
        'save': save,
        'processValidationErrors': processValidationErrors
      };
    }
  ]);
});
