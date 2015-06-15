'use strict';

angular.module('alienUiApp').factory('propertiesServices', ['$resource',
  function($resource) {

    // check property
    var checkProperty = $resource('rest/properties/check', {}, {
      'check': {
        method: 'POST',
        isArray: false,
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });

    return {
      'validConstraints': checkProperty.check
    };

  }
]);