define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');
  var angular = require('angular');

  require('scripts/common/directives/facet_search_panel');
  require('scripts/common/directives/pagination');
  require('scripts/components/services/repository');

  states.state('components.repositories', {
    url: '/repositories',
    template: '<ui-view/>',
    controller: 'LayoutCtrl',
    menu: {
      id: 'cm.components.repositories.list',
      state: 'components.repositories.list',
      key: 'NAVBAR.MENU_REPOSITORIES',
      icon: 'fa fa-github',
      priority: 20,
      roles: ['COMPONENTS_MANAGER']
    }
  });
  states.state('components.repositories.list', {
    url: '/list',
    templateUrl: 'views/components/repository_list.html',
    controller: 'RepositoryListCtrl',
    resolve: {
      repositoryPlugins: ['repositoryPluginService', function (repositoryPluginService) {
        return repositoryPluginService.get({}, undefined).$promise.then(function (response) {
          return response.data;
        });
      }]
    }
  });
  states.forward('components.repositories', 'components.repositories.list');

  var NewRepositoryCtrl = ['$scope', '$modalInstance',
    function ($scope, $modalInstance) {

      $scope.repository = {};

      $scope.repositoryTypes = _.map($scope.repositoryPlugins, 'repositoryType');

      $scope.repository.type = $scope.repositoryTypes.length > 0 ? $scope.repositoryTypes[0] : undefined;

      $scope.step = 1;

      $scope.maxStep = 2;

      $scope.cancel = function () {
        $modalInstance.dismiss('cancel');
      };

      $scope.next = function (valid) {
        if (valid) {
          $scope.step++;
          if ($scope.step > $scope.maxStep) {
            $modalInstance.close($scope.repository);
          }
        }
      };
    }];

  modules.get('a4c-components', ['ui.router', 'ui.bootstrap']).controller('RepositoryListCtrl', ['$scope', '$modal', 'repositoryService', 'repositoryPlugins',
    function ($scope, $modal, repositoryService, repositoryPlugins) {
      $scope.repositoryPlugins = repositoryPlugins;
      $scope.onSearch = function (searchConfig) {
        $scope.searchConfig = searchConfig;
      };

      $scope.openCreateRepositoryModal = function () {
        var modalInstance = $modal.open({
          templateUrl: 'views/components/repository_new.html',
          controller: NewRepositoryCtrl,
          scope: $scope
        });
        modalInstance.result.then(function (application) {
          // create a new application from the given name and description.
          repositoryService.create({}, angular.toJson(application), function (response) {
            console.log('Received', response);
          });
        });
      };
    }]);
});