define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');

  require('scripts/components/services/csar');
  require('scripts/components/services/csar_git');
  require('scripts/components/controllers/csar_details');
  require('scripts/common/directives/pagination');

  states.state('components.csars', {
    url: '/csars',
    template: '<ui-view/>',
    menu: {
      id: 'cm.components.csars.list',
      state: 'components.csars.list',
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

  var NewCsarGitController = ['$scope', '$modalInstance',
    function($scope, $modalInstance) {
      $scope.csarGitTemplate = {};
      $scope.create = function(valid, csargit) {
        if(!valid) {
          return;
        }
        var CsarCheckoutLocations={
          'branchId': csargit.branchId,
          'subPath': csargit.archive
        };
        csargit.locations=CsarCheckoutLocations;
        var csargitDTO ={
          'username': 'empty',
          'password': 'empty',
          'repositoryUrl': csargit.url,
          'importLocations': [CsarCheckoutLocations]
        };
        $modalInstance.close(csargitDTO);
      };
      $scope.cancel = function() {
        $modalInstance.dismiss('cancel');
      };
    }
  ];

  /* Main CSAR search controller */
  modules.get('a4c-components', ['ui.router', 'ui.bootstrap']).controller('CsarListCtrl', ['$scope', '$modal', '$state', 'csarService', 'csarGitService', '$translate', 'toaster',
   function($scope, $modal, $state, csarService, csarGitService, $translate, toaster) {
      $scope.search = function() {
        var searchRequestObject = {
          'query': $scope.query,
          'from': 0,
          'size': 50
        };
        $scope.csarSearchResult = csarService.searchCsar.search([], angular.toJson(searchRequestObject));
      };

      $scope.searchCsarsGit = function() {
        var searchRequestObject = {
          'query': $scope.queryCsarGit,
          'from': 0,
          'size': 50
        };
        $scope.csarGitSearchResult = csarGitService.search([],angular.toJson(searchRequestObject));
      };

      $scope.triggerImport = function(id) {
        csarGitService.fetch({
          id: id
        }, angular.toJson(id), function() {
          $scope.search();
       });
      };

      $scope.triggerImportAllCsarGit = function(data) {
        if (data.length > 0) {
          for (var i=0; i<data.length; i++) {
            csarGitService.fetch({
              id: data[i].id
            }, angular.toJson(data[i].id), function() {
              $scope.search();
           });
          }
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
            var title = $translate('CSAR.ERRORS.' + result.error.code + '_TITLE');
            toaster.pop('error', title, errorMessage, 4000, 'trustedHtml', null);
          }
          // refresh csar list
          $scope.search();
        });
      };

      $scope.removeCsarGit = function(id) {
        csarGitService.remove({
          id: id
        }, function() {
          // refresh csargit list
          $scope.searchCsarsGit();
        });
      };


      $scope.openNewCsarGitTemplate = function() {
        var modalInstance = $modal.open({
          templateUrl: 'newCsarGit.html',
          controller: NewCsarGitController,
          scope: $scope
        });
        modalInstance.result.then(function(csarGitTemplate) {
          csarGitService.create([], angular.toJson(csarGitTemplate), function(successResponse) {
           $scope.searchCsarsGit();
          });
        });
      };

      // init search
      $scope.search();
      $scope.searchCsarsGit();
    }
  ]); // controller
}); // define
