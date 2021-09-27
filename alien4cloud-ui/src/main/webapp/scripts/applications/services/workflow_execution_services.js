define(function (require) {
  'use strict';

  var modules = require('modules');
  var angular = require('angular');

  modules.get('a4c-applications', ['ngResource']).factory('workflowExecutionServices', ['$resource',
    function ($resource) {
      // Search for application environments

      var workflowExecutionPerExecution = $resource('rest/latest/workflow_execution/:deploymentId', {}, {
        'get': {
          method: 'GET'
        },
        'resume': {
          method: 'PATCH',
          params: {
            deploymentId : '@deploymentId'
          }
        }
      });

      return {
        'get': workflowExecutionPerExecution.get,
        'resume': workflowExecutionPerExecution.resume
      };
    }
  ]);
});
