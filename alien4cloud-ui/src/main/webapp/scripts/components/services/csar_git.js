define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-components', ['ngResource']).factory('csarGitService', ['$resource', '$translate', function($resource, $translate) {

    var remove = $resource('rest/csarsgit/:id',{},{},{
        'remove':{
          method: 'DELETE',
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
        },
      }
    });

    var search = $resource('rest/csarsgit/search', {}, {
      'search': {
        method: 'POST',
        isArray: false,
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });

    var create = $resource('rest/csarsgit',{},{
       'create':{
         method: 'POST',
         isArray: false,
         headers: {
           'Content-Type': 'application/json; charset=UTF-8'
         }
       }
     });

     var update = $resource('rest/csarsgit/:id',{},{
        'update':{
          method: 'PUT',
          isArray: false,
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          }
        }
      });

     var fetch = $resource('rest/csarsgit/import/:id',{}, {
        'import':{
          method: 'POST',
          isArray: false,
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          }
        }
      });

      return {
        'remove': remove.remove,
        'search': search.search,
        'create': create.create,
        'fetch': fetch.import,
        'update':update.update
      };
  }]);
});
