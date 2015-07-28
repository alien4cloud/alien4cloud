define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');

  require('scripts/components/services/csar');
  require('scripts/components/controllers/csar_details');
  require('scripts/common/directives/pagination');

  states.state('components.csars', {
    url: '/csars',
    template: '<ui-view/>',
    menu: {
      id: 'cm.components.csars',
      state: 'components.csars',
      key: 'NAVBAR.MENU_CSARS',
      icon: 'fa fa-archive',
      priority: 20
    }
  });
  states.state('components.csars.list', {
    url: '/list',
    templateUrl: 'views/components/csar_list.html',
    controller: 'CsarListCtrl'
  });
  states.forward('components.csars', 'components.csars.list');

  /* Main CSAR search controller */
  modules.get('a4c-components', ['ui.router', 'ui.bootstrap']).controller('CsarListCtrl', ['$scope', '$modal', '$state', 'csarService', '$translate', 'toaster',
    function($scope, $modal, $state, csarService, $translate, toaster) {
      $scope.search = function() {
        var searchRequestObject = {
          'query': $scope.query,
          'from': 0,
          'size': 50
        };
        $scope.csarSearchResult = csarService.searchCsar.search([], angular.toJson(searchRequestObject));
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
            var title = $translate('CSAR.ERRORS.' + result.error.code + '_TITLE');
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
