define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');

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
    ['$scope', '$state', '$stateParams', 'userContextServices', 'applicationServices', 'applicationEnvironmentServices',
    function ($scope, $state, $stateParams, userContextServices, applicationServices, applicationEnvironmentServices) {

      applicationServices.get({ applicationId: $stateParams.applicationId }, function(result) {
        $scope.application = result.data;
      });

      applicationEnvironmentServices.get({
        applicationId: $stateParams.applicationId,
        applicationEnvironmentId: $stateParams.environmentId
      }, function (result) {
        $scope.environment = result.data;
      });

      $scope.goToApplication = function($event) {
        $event.preventDefault();
        $event.stopPropagation();
        userContextServices.clear($stateParams.applicationId);
        $state.go('applications.detail', { id: $stateParams.applicationId });
      };

      $scope.goToEnvironment = function($event) {
        $event.preventDefault();
        $event.stopPropagation();
        $state.go('applications.detail.environment.deploynext.topology', {
          id: $stateParams.applicationId,
          environmentId: $stateParams.environmentId,
        });
      };
    }
  ]);
});
