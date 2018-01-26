define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');
  var angular = require('angular');

  require('angular-ui-ace');
  require('scripts/common/directives/ace_save_button');
  require('scripts/_ref/applications/services/application_var_git_service');

  states.state('applications.detail.variables', {
    url: '/variables',
    templateUrl: 'views/_ref/applications/applications_detail_variables.html',
    controller: 'ApplicationVariablesCtrl',
    resolve: {
      userCanModify: ['authService', function(authService) { return authService.hasRole('APPLICATIONS_MANAGER'); }]
    },
    menu: {
      id: 'am.applications.detail.variables',
      state: 'applications.detail.variables',
      key: 'NAVAPPLICATIONS.MENU_VARIABLES',
      icon: 'fa fa-usd',
      roles: ['APPLICATION_MANAGER'],
      priority: 400
    }
  });

  var EditAppVarGitCtrl = ['$scope', '$uibModalInstance', 'appVarGitService', 'gitLocation',
    function($scope, $uibModalInstance, appVarGitService, gitLocation) {
      $scope.originalGitLocation = gitLocation;
      $scope.gitLocation = _.cloneDeep(gitLocation);
      if($scope.gitLocation.local) {
        $scope.gitLocation.url = 'http://';
      }

      $scope.save = function(valid) {
        if (valid && !_.isEqual($scope.originalGitLocation, $scope.gitLocation)) {
          if($scope.gitLocation.local) {
            appVarGitService.delete({
              applicationId: $scope.application.id
            });
          } else {
            var request = {
              url: $scope.gitLocation.url,
              username: _.get($scope.gitLocation, 'credential.username'),
              password: _.get($scope.gitLocation, 'credential.password'),
              path: $scope.gitLocation.path,
              branch: $scope.gitLocation.branch
            };

            appVarGitService.create({
              applicationId: $scope.application.id
            }, angular.toJson(request));
          }
          $uibModalInstance.close($scope.gitLocation);
        }
      };

      $scope.cancel = function() {
        $uibModalInstance.dismiss('cancel');
      };
    }
  ];

  modules.get('a4c-applications').controller('ApplicationVariablesCtrl', ['$scope', '$translate', '$alresource', '$uibModal', 'authService', 'breadcrumbsService', 'application',
    function($scope, $translate, $alresource, $uibModal, authService, breadcrumbsService, applicationResponse) {
      breadcrumbsService.putConfig({
        state : 'applications.detail.variables',
        text: function(){
          return $translate.instant('NAVAPPLICATIONS.MENU_VARIABLES');
        }
      });

      $scope.application = applicationResponse.data;
      $scope.isManager = authService.hasResourceRole($scope.application, 'APPLICATION_MANAGER');

      var aceEditor;
      // aceEditor.getSession().setValue(result.data);
      var variablesService = $alresource('/rest/applications/:applicationId/variables');

      function setAceEditorContent (content){
        $scope.editorContent = {old: content, new: content};
      }
      $scope.saveFile = function() {
        variablesService.create({
          applicationId: $scope.application.id
        }, {content: aceEditor.getSession().getDocument().getValue()}, function(){
          setAceEditorContent($scope.editorContent.new);
        });
      };

      $scope.load = function() {
        variablesService.get({applicationId: $scope.application.id}).$promise.then(function(result){
          setAceEditorContent(result.data);
        });
      };

      $scope.aceLoaded = function(_editor) {
        aceEditor = _editor;
        _editor.commands.addCommand({
          name: 'save',
          bindKey: {win: 'Ctrl-S', mac: 'Command-S'},
          exec: function() {
            $scope.saveFile();
          }
        });
        $scope.load();
      };

      // Modal to configure a custom git
      $scope.editGit = function() {
        $uibModal.open({
          templateUrl: 'views/_ref/applications/applications_detail_environments_git.html',
          controller: EditAppVarGitCtrl,
          scope: $scope,
          resolve: {
            gitLocation: ['appVarGitService', function (appVarGitService) {
              return _.catch(function () {
                return appVarGitService.get({
                  applicationId: $scope.application.id
                }).$promise.then(function (result) {
                  return result.data;
                });
              });
            }]
          }
        });
      };
    }
  ]);
}); // define
