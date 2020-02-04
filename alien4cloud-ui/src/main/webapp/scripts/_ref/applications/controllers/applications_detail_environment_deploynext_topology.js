define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');

  require('scripts/_ref/applications/controllers/applications_detail_environment_deploynext_topology_editor');

  require('scripts/topology/directives/topology_validation_display');
  require('scripts/topology/directives/topology_rendering');

  states.state('applications.detail.environment.deploynext.topology', {
    url: '/topology',
    templateUrl: 'views/_ref/applications/applications_detail_environment_deploynext_topology.html',
    controller: 'AppEnvDeployNextTopologyCtrl',
    menu: {
      id: 'applications.detail.environment.deploynext.topology',
      state: 'applications.detail.environment.deploynext.topology',
      key: 'NAVAPPLICATIONS.MENU_DEPLOY_NEXT_TOPOLOGY',
      icon: '',
      priority: 200,
      step: {
        taskCodes: ['EMPTY', 'VALIDATION_PLUGIN', 'IMPLEMENT_RELATIONSHIP', 'SATISFY_LOWER_BOUND', 'PROPERTIES',
                    'SCALABLE_CAPABILITY_INVALID', 'NODE_FILTER_INVALID', 'WORKFLOW_INVALID', 'ARTIFACT_INVALID'],
        source: 'topology'
      }
    }
  });

  modules.get('a4c-applications').controller('AppEnvDeployNextTopologyCtrl',
    ['$scope', '$state', 'authService', 'resizeServices', 'breadcrumbsService', '$translate',
    function ($scope, $state, authService, resizeServices, breadcrumbsService, $translate) {

      breadcrumbsService.putConfig({
        state : 'applications.detail.environment.deploynext.topology',
        text: function(){
          return $translate.instant('NAVAPPLICATIONS.MENU_DEPLOY_NEXT_TOPOLOGY');
        },
        onClick: function(){
          $state.go('applications.detail.environment.deploynext.topology', {
            id: $scope.application.id,
            environmentId: $scope.environment.id
          });
        }
      });


      // Filter tasks to match only the screen task codes
      $scope.canEditTopology = authService.hasResourceRoleIn($scope.application, ['APPLICATION_MANAGER', 'APPLICATION_DEVOPS']);

      $scope.topologyValidationDTO = _.get($scope.deploymentTopologyDTO, 'validation.bySources.topology');

      // Filter the errors that have a topology source
      $scope.topologyBox = {
        width: 600,
        height: 300
      };

      resizeServices.registerContainer(function(width, height) {
        $scope.topologyBox = {
          width: width - 10,
          height: height - 10
        };
        $scope.$digest();
      }, '#topology-preview');

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
