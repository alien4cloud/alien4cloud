define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');

  require('scripts/topology/directives/topology_validation_display');
  require('scripts/_ref/applications/controllers/applications_detail_environment_deploynext_topology_editor');

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
        taskCodes: ['EMPTY', 'IMPLEMENT_RELATIONSHIP', 'SATISFY_LOWER_BOUND', 'PROPERTIES', 'SCALABLE_CAPABILITY_INVALID', 'NODE_FILTER_INVALID', 'WORKFLOW_INVALID'],
        source: 'topology'
      }
    }
  });

  modules.get('a4c-applications').controller('AppEnvDeployNextTopologyCtrl',
    ['$scope', '$state', 'authService',
    function ($scope, $state, authService) {
      // Filter tasks to match only the screen task codes
      $scope.canEditTopology = authService.hasResourceRoleIn($scope.application, ['APPLICATION_MANAGER', 'APPLICATION_DEVOPS']);

      $scope.editTopology = function() {
        $state.go('editor_app_env.editor', {
          applicationId: $scope.application.id,
          environmentId: $scope.environment.id,
          archiveId: $scope.application.id + ':' + $scope.environment.currentVersionName
        });
      };
    }
  ]);
});
