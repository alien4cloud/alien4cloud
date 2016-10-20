define(function (require) {
  'use strict';
  
  var modules = require('modules');
  var states = require('states');
  
  require('scripts/components/services/csar');
  require('scripts/components/controllers/csar_details');
  require('scripts/common/directives/pagination');
  require('scripts/authentication/services/authservices');
  
  states.state('components.csars', {
    url: '/csars',
    template: '<ui-view/>',
    controller: 'LayoutCtrl',
    menu: {
      id: 'cm.components.csars.list',
      state: 'components.csars.list',
      key: 'NAVBAR.MENU_CSARS',
      icon: 'fa fa-archive',
      priority: 30,
      roles: ['COMPONENTS_BROWSER']
    }
  });
  states.state('components.csars.list', {
    url: '/list',
    templateUrl: 'views/components/csar_list.html',
    controller: 'CsarListCtrl'
  });
  states.forward('components.csars', 'components.csars.list');
  
  /* Main CSAR search controller */
  modules.get('a4c-components', ['ui.router', 'ui.bootstrap']).controller('CsarListCtrl', ['$scope', '$modal', '$state', 'csarService', '$translate', 'toaster', 'authService',
    function ($scope, $modal, $state, csarService, $translate, toaster, authService) {
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
        $state.go('components.csars.csardetail', {csarId: csarId});
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
          $scope.searchConfig.service.search()();
        });
      };
    }
  ]); // controller
}); // define
