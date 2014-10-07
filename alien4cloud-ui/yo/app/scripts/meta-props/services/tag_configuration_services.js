'use strict';

angular.module('alienUiApp').factory('tagConfigurationServices', ['$resource',
  function($resource) {

    var TagConfigurationDAO = $resource('rest/tagconfigurations/:id');

    var searchTagConfiguration = $resource('rest/tagconfigurations/search', {}, {
      'search': {
        method: 'POST',
        isArray: false,
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });


    var get = function(id) {
      return TagConfigurationDAO.get({
        'id': id
      }).$promise.then(function(result) {
          return result.data;
        });
    };

    var remove = function(id) {
      return TagConfigurationDAO.remove({
        'id': id
      }).$promise.then(function(result) {
          return result.data;
        });
    };

    var save = function(config) {
      return TagConfigurationDAO.save({}, angular.toJson(config)).$promise.then(function(result) {
        return result.data;
      });
    };

    var processValidationErrors = function(validationErrors) {
      if (validationErrors) {
        var errors = {};
        for (var i = 0; i < validationErrors.length; i++) {
          var error = validationErrors[i];
          if (UTILS.isArrayDefinedAndNotEmpty(errors[error.error])) {
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
      'search': searchTagConfiguration.search,
      'get': get,
      'remove': remove,
      'save': save,
      'processValidationErrors': processValidationErrors
    };
  }
]);