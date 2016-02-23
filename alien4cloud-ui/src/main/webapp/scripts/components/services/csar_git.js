define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-components', ['ngResource']).factory('csarGitService', ['$resource', function($resource) {

    var remove = $resource('rest/csarsgit/:id',{},{},{
      'remove':{
        method: 'DELETE',
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

     var fetch = $resource('rest/csarsgit/:id',{}, {
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
        'create': create.create,
        'fetch': fetch.import,
        'update':update.update
      };
  }]);
});
