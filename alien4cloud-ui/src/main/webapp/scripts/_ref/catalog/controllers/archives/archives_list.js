// components list: browse and search for components
define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');


  // load other locations to manage archives
  require('scripts/components/services/csar');
  require('scripts/_ref/catalog/controllers/archives/archives_detail');
  require('scripts/_ref/catalog/controllers/archives/archives_git');
  require('scripts/common/directives/pagination');
  require('scripts/authentication/services/authservices');
  require('scripts/_ref/common/directives/search');


  states.state('catalog.archives.list', {
    url: '/list',
    templateUrl: 'views/_ref/catalog/archives/archives_list.html',
    controller: 'ArchivesListCtrl',
  });

  modules.get('a4c-catalog', ['ui.router', 'ui.bootstrap']).controller('ArchivesListCtrl', ['$scope', '$state', 'csarService', '$translate', 'toaster', 'authService', 'searchServiceFactory',
    function ($scope, $state, csarService, $translate, toaster, authService, searchServiceFactory) {

      $scope.writeWorkspaces = [];
      var isComponentManager = authService.hasOneRoleIn(['COMPONENT_MANAGER', 'ARCHITECT']);
      if (isComponentManager === true) {
        $scope.writeWorkspaces.push({id:'ALIEN_GLOBAL_WORKSPACE'});
      } else if (isComponentManager.hasOwnProperty('then')) {
        isComponentManager.then(function (hasRole) {
          if (hasRole) {
            $scope.writeWorkspaces.push({id:'ALIEN_GLOBAL_WORKSPACE'});
          }
        });
      }

       $scope.queryManager = {labelPrefix: 'COMPONENTS.CSAR.'};
       $scope.searchService = searchServiceFactory('rest/latest/csars/search', false, $scope.queryManager, 20);

      $scope.openCsar = function (csarId) {
        $state.go('catalog.archives.detail', {id: csarId});
      };

      // remove a csar
      $scope.remove = function (csarId) {
        csarService.getAndDeleteCsar.remove({
          csarId: csarId
        }, function (result) {
          var errorMessage = csarService.builtErrorResultList(result);
          if (errorMessage) {
            var title = $translate.instant('CSAR.ERRORS.' + result.error.code + '_TITLE');
            toaster.pop('error', title, errorMessage, 4000, 'trustedHtml', null);
          }
          // refresh csar list
          $scope.searchService.search();
        });
      };

      $scope.uploadSucessCalback = function(){
        $scope.searchService.search();
      };

       //go to git import management
       $scope.goToGitImportManagement = function() {
         $state.go('catalog.archives.git');
       };

    }
  ]); // controller
});
