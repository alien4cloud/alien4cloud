define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');

  var registerEditorSubstates = require('scripts/topology/editor_register_service');

  states.state('editor_app_version', {
    url: '/editor/application/:applicationId/version/:versionId/archive/:archiveId',
    templateUrl: 'views/_ref/applications/applications_detail_version_editor.html',
    controller: 'AppVersionTopoEditorCtrl'
  });

  // Define editor states from root (to use full-screen and avoid dom and scopes pollution)
  states.state('editor_app_version.editor', {
    url: '/edit',
    templateUrl: 'views/topology/topology_editor_layout.html',
    controller: 'TopologyEditorCtrl'
  });
  registerEditorSubstates('editor_app_version.editor');
  states.forward('editor_app_version', 'editor_app_version.editor');

  modules.get('a4c-applications').controller('AppVersionTopoEditorCtrl',
    ['$scope', '$state', '$stateParams', 'applicationServices', 'applicationVersionServices', 'breadcrumbsService','$translate',
    function ($scope, $state, $stateParams, applicationServices, versionServices, breadcrumbsService) {

      breadcrumbsService.registerMapping('editor_app_version.', 'applications.detail.version.editor');
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
          state: 'applications.detail.version',
          text: function () {
            return scope.version.version;
          },
          onClick: function () {
            $state.go('applications.detail.version', {
              id: $scope.application.id,
              versionId: $scope.version.id
            });
          }
        });

        breadcrumbsService.putConfig({
          state: 'applications.detail.version.editor',
          text: function () {
            return 'Editor (' + $stateParams.archiveId + ')';
          },
          onClick: function () {
            $state.go('editor_app_env.editor', {
              id: $scope.application.id,
              versionId: $scope.version.id
            });
          }
        });
      };

      var andVersionAvailable = function(){
        return _.defined($scope.application) && _.defined($scope.version);
      };

      applicationServices.get({ applicationId: $stateParams.applicationId }, function(result) {
        $scope.application = result.data;
        if(andVersionAvailable()) {
          setupBreadCrumbs($scope);
        }
      });

      versionServices.get({
        delegateId: $stateParams.applicationId,
        versionId: $stateParams.versionId
      }, function (result) {
        $scope.version = result.data;

        if(andVersionAvailable()){
          setupBreadCrumbs($scope);
        }
      });

      $scope.$emit('$contextPush', {type: 'Application', data: {applicationId: $stateParams.applicationId}});
      $scope.$on('$destroy', function() {
        $scope.$emit('$contextPoll');
      });
    }
  ]);
});
