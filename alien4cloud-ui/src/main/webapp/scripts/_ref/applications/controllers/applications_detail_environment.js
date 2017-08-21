/*
* Application list is the entry point for the application management.
*/
define(function (require) {
  'use strict';

  var states = require('states');
  var modules = require('modules');

  states.state('applications.detail.environment', {
    url: '/environment/:environmentId',
    templateUrl: 'views/_ref/applications/applications_detail_environment.html',
    controller: 'ApplicationEnvironmentCtrl',
    resolve: {
      environment: ['appEnvironments', '$stateParams',
        function(appEnvironments, $stateParams) {
          // Select the environment based on it's id
          console.log(appEnvironments, $stateParams.id, $stateParams.environmentId);
          return {};
        }
      ]
    },
    params: {
      // optional id of the environment to automatically select when triggering this state
      environmentId: null
    }
  });

  modules.get('a4c-applications').controller('ApplicationEnvironmentCtrl',
    ['$scope',
    function ($scope) {
      console.log('init env controller');
    }
  ]);
});
