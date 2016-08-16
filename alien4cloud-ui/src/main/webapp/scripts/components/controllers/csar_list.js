define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');

  require('scripts/components/services/csar');
  require('scripts/components/controllers/csar_details');
  require('scripts/common/directives/pagination');

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
      roles: ['COMPONENTS_MANAGER']
    }
  });
  states.state('components.csars.list', {
    url: '/list',
    templateUrl: 'views/components/csar_list.html',
    controller: 'CsarListCtrl'
  });
  states.forward('components.csars', 'components.csars.list');

  /* Main CSAR search controller */
  modules.get('a4c-components', ['ui.router', 'ui.bootstrap']).controller('CsarListCtrl', ['$scope', '$modal', '$state', 'csarService', '$translate', 'toaster', 'searchServiceFactory',
   function($scope, $modal, $state, csarService, $translate, toaster, searchServiceFactory) {

      $scope.searchService = searchServiceFactory('rest/latest/csars/search', false, $scope, 20, 10);

      $scope.search = function() {
        $scope.searchService.search();
      };

      //on search completed
      $scope.onSearchCompleted = function(searchResult) {
        if(_.undefined(searchResult.error)) {
          $scope.searchResult = searchResult.data;
        } else {
          console.log('error when searching...', searchResult.error);
        }
      };

      $scope.openCsar = function(csarId) {
        $state.go('components.csars.csardetail', { csarId: csarId });
      };

      // remove a csar
      $scope.remove = function(csarId) {
        csarService.getAndDeleteCsar.remove({
          csarId: csarId
        }, function(result) {
          var errorMessage = csarService.builtErrorResultList(result);
          if (errorMessage) {
            var title = $translate.instant('CSAR.ERRORS.' + result.error.code + '_TITLE');
            toaster.pop('error', title, errorMessage, 4000, 'trustedHtml', null);
          }
          // refresh csar list
          $scope.search();
        });
      };

      // init search
      $scope.search();
    }
  ]); // controller
}); // define
