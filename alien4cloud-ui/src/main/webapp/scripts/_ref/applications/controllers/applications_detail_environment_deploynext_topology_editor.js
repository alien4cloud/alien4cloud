define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');

  var registerEditorSubstates = require('scripts/topology/editor_register_service');

  states.state('editor_app_env', {
    url: '/editor/application/:applicationId/environment/:environmentId',
    templateUrl: 'views/_ref/applications/applications_detail_environment_deploynext_topology_editor.html',
    controller: 'AppEnvTopoEditorCtrl'
  });

  // Define editor states from root (to use full-screen and avoid dom and scopes pollution)
  states.state('editor_app_env.editor', {
    url: '/archive/:archiveId',
    templateUrl: 'views/topology/topology_editor_layout.html',
    controller: 'TopologyEditorCtrl'
  });
  registerEditorSubstates('editor_app_env.editor');
  states.forward('editor_app_env', 'editor_app_env.editor');

  modules.get('a4c-applications').controller('AppEnvTopoEditorCtrl',
    ['$scope', '$state', '$stateParams', 'applicationServices', 'applicationEnvironmentServices', 'breadcrumbsService','$translate', 'topoEditDisplay',
    function ($scope, $state, $stateParams, applicationServices, applicationEnvironmentServices, breadcrumbsService, $translate, topoEditDisplay) {
      topoEditDisplay($scope, '#editor-explorer');

      breadcrumbsService.registerMapping('editor_app_env.', 'applications.detail.environment.deploynext.topology.');
      var setupBreadCrumbs = function (scope) {
        breadcrumbsService.putConfig({
          state: 'applications.detail',
          text: function () {
            return scope.application.name;
          },
          onClick: function () {
            $state.go('applications.detail', { id: $scope.application.id });
          }
        });

        breadcrumbsService.putConfig({
          state: 'applications.detail.environment',
          text: function () {
            return scope.environment.name;
          },
          onClick: function () {
            $state.go('applications.detail.environment', {
              id: $scope.application.id,
              environmentId: $scope.environment.id
            });
          }
        });

        breadcrumbsService.putConfig({
          state: 'applications.detail.environment.deploynext.topology',
          text: function () {
            return $translate.instant('NAVAPPLICATIONS.MENU_DEPLOY_NEXT_TOPOLOGY');
          },
          onClick: function () {
            $state.go('applications.detail.environment.deploynext.topology', {
              id: scope.application.id,
              environmentId: scope.environment.id
            });
          }
        });

        breadcrumbsService.putConfig({
          state: 'applications.detail.environment.deploynext.topology.editor',
          text: function () {
            return 'Editor (' + scope.environment.currentVersionName + ')';
          },
          onClick: function () {
            $state.go('editor_app_env.editor', {
              applicationId: scope.application.id,
              environmentId: scope.environment.id,
              archiveId: scope.application.id + ':' + scope.environment.currentVersionName
            });
          }
        });
      };

      var appAndEnvAvailable = function(){
        return _.defined($scope.application) && _.defined($scope.environment);
      };

      applicationServices.get({ applicationId: $stateParams.applicationId }, function(result) {
        $scope.application = result.data;

        if(appAndEnvAvailable()){
          setupBreadCrumbs($scope);
        }
      });

      applicationEnvironmentServices.get({
        applicationId: $stateParams.applicationId,
        applicationEnvironmentId: $stateParams.environmentId
      }, function (result) {
        $scope.environment = result.data;

        if(appAndEnvAvailable()){
          setupBreadCrumbs($scope);
        }
      });
    }
  ]);
});
