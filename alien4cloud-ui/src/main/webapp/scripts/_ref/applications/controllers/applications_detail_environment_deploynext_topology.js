define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

  require('scripts/topology/directives/topology_validation_display');

  states.state('applications.detail.environment.deploynext.topology', {
    url: '/topology',
    templateUrl: 'views/_ref/applications/applications_detail_environment_deploynext_topology.html',
    controller: 'AppEnvDeployNextTopologyCtrl',
    menu: {
      id: 'applications.detail.environment.deploynext.topology',
      state: 'applications.detail.environment.deploynext.topology',
      key: 'NAVAPPLICATIONS.MENU_DEPLOY_NEXT.TOPOLOGY',
      icon: '',
      priority: 200,
      step: {
        taskCodes: ['EMPTY', 'IMPLEMENT_RELATIONSHIP', 'SATISFY_LOWER_BOUND', 'PROPERTIES', 'SCALABLE_CAPABILITY_INVALID', 'NODE_FILTER_INVALID', 'WORKFLOW_INVALID']
      }
    }
  });

  modules.get('a4c-applications').controller('AppEnvDeployNextTopologyCtrl',
    ['$scope',
    function ($scope) {
      // Filter tasks to match only the screen task codes
      $scope.canEditTopology = true;
    }
  ]);
});
