define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-metas').factory('orchestratorPropertiesServices', ['$resource',
    function($resource) {
      var orchestratorProperties = $resource('rest/orchestrators/:id/properties', {}, {
        'upsert': {
          method: 'POST',
          isArray: false,
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          }
        }
      });

      return {
        'upsertProperty': orchestratorProperties.upsert
      };
    }]
  );
});
