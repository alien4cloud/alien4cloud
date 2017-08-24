define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');

  require('scripts/topology/directives/topology_validation_display');
  var registerService = require('scripts/topology/editor_register_service');

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

  // TODO create a sub-state to load the environment name and have the breadcrumb there.
  // Define editor states from root (to use full-screen and avoid dom and scopes pollution)
  states.state('editor_application_environment', {
    url: '/editor/application/:applicationId/environment/:environmentId/archive/:archiveId',
    templateUrl: 'views/topology/topology_editor_layout.html',
    controller: 'TopologyEditorCtrl',
    resolve: {
      context: ['$stateParams', function($stateParams) {
        return {
          applicationId: $stateParams.applicationId,
          environmentId: $stateParams.environmentId,
          archiveId: $stateParams.archiveId
        };
      }]
    }
  });

  registerService('editor_application_environment');

  modules.get('a4c-applications').controller('AppEnvDeployNextTopologyCtrl',
    ['$scope', '$state',
    function ($scope, $state) {
      // Filter tasks to match only the screen task codes
      $scope.canEditTopology = true;

      $scope.editTopology = function() {
        $state.go('editor_application_environment', {
          applicationId: $scope.application.id,
          environmentId: $scope.environment.id,
          archiveId: $scope.application.id + ':' + $scope.environment.currentVersionName
        });
      };
    }
  ]);
});
