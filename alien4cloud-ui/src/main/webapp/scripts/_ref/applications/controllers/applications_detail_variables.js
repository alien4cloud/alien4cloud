define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');

  require('angular-ui-ace');

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

  modules.get('a4c-applications').controller('ApplicationVariablesCtrl', ['$scope', '$translate', '$alresource', 'authService', 'breadcrumbsService', 'application',
    function($scope, $translate, $alresource, authService, breadcrumbsService, applicationResponse) {
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

      $scope.saveFile = function() {
        variablesService.create({
          applicationId: $scope.application.id
        }, {content: aceEditor.getSession().getDocument().getValue()});
      };

      $scope.load = function() {
        variablesService.get({applicationId: $scope.application.id}).$promise.then(function(result){
          aceEditor.getSession().setValue(result.data);
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
    }
  ]);
}); // define
