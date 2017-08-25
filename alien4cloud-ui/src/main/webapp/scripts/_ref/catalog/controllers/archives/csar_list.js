// components list: browse and search for components
define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');

  // load other locations to manage archives
  require('scripts/components/services/csar');
  require('scripts/_ref/catalog/controllers/archives/csar_detail');
  require('scripts/common/directives/pagination');
  require('scripts/authentication/services/authservices');

  states.state('catalog.archives.list', {
    url: '/list',
    templateUrl: 'views/_ref/catalog/archives/csar_list.html',
    controller: 'CsarListCtrl',
  });


  /* Main CSAR search controller */
  modules.get('a4c-components', ['ui.router', 'ui.bootstrap']).controller('CsarListCtrl', ['$scope', '$uibModal', '$state', 'csarService', '$translate', 'toaster', 'authService',
    function ($scope, $uibModal, $state, csarService, $translate, toaster, authService) {
      $scope.writeWorkspaces = [];
      var isComponentManager = authService.hasOneRoleIn(['COMPONENT_MANAGER', 'ARCHITECT']);
      if (isComponentManager === true) {
        $scope.writeWorkspaces.push('ALIEN_GLOBAL_WORKSPACE');
      } else if (isComponentManager.hasOwnProperty('then')) {
        isComponentManager.then(function (hasRole) {
          if (hasRole) {
            $scope.writeWorkspaces.push('ALIEN_GLOBAL_WORKSPACE');
          }
        });
      }

      $scope.onSearch = function (searchConfig) {
        $scope.searchConfig = searchConfig;
      };

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
          $scope.searchConfig.service.search();
        });
      };

    }
  ]); // controller
});
