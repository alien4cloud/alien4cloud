define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-common').factory('propertiesServices', ['$resource',
    function($resource) {
      // check property
      var checkProperty = $resource('rest/latest/properties/check', {}, {
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
});
