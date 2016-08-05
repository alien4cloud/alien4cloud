define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');
  var angular = require('angular');

  require('scripts/common/directives/facet_search_panel');
  require('scripts/common/directives/pagination');
  require('scripts/components/services/repository');
  require('scripts/common/directives/generic_form');

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
    controller: 'RepositoryListCtrl'
  });
  states.forward('components.repositories', 'components.repositories.list');

  var NewRepositoryCtrl = ['$scope', '$modalInstance', 'repositoryPlugins', 'repositoryPluginConfigurationService',
    function ($scope, $modalInstance, repositoryPlugins, repositoryPluginConfigurationService) {

      $scope.repositoryPlugins = repositoryPlugins;

      $scope.repository = {configuration: {}};

      $scope.repositoryTypes = _.map($scope.repositoryPlugins, 'repositoryType');

      $scope.repository.type = $scope.repositoryTypes.length > 0 ? $scope.repositoryTypes[0] : undefined;

      $scope.step = 1;

      $scope.maxStep = 2;

      $scope.cancel = function () {
        $modalInstance.dismiss('cancel');
      };

      var findPluginId = function (repositoryType) {
        return _.find($scope.repositoryPlugins, function (plugin) {
          return plugin.repositoryType === repositoryType;
        }).pluginComponent.pluginId;
      };

      $scope.prepareStep = function (forStep) {
        switch (forStep) {
          case 2:
            $scope.repository.pluginId = findPluginId($scope.repository.type);
            repositoryPluginConfigurationService.get({
              pluginId: $scope.repository.pluginId
            }, undefined, function (response) {
              $scope.configurationDefinition = response.data;
            });
        }
      };

      $scope.next = function (valid) {
        if (valid) {
          $scope.prepareStep(++$scope.step);
          if ($scope.step > $scope.maxStep) {
            $modalInstance.close($scope.repository);
          }
        }
      };
    }];

  modules.get('a4c-components', ['ui.router', 'ui.bootstrap']).controller('RepositoryListCtrl', ['$scope', '$modal', 'repositoryService',
    function ($scope, $modal, repositoryService) {
      $scope.onSearch = function (searchConfig) {
        $scope.searchConfig = searchConfig;
      };

      $scope.openCreateRepositoryModal = function () {
        var modalInstance = $modal.open({
          templateUrl: 'views/components/repository_new.html',
          controller: NewRepositoryCtrl,
          resolve: {
            repositoryPlugins: ['repositoryPluginService', function (repositoryPluginService) {
              return repositoryPluginService.get({}, undefined).$promise.then(function (response) {
                return response.data;
              });
            }]
          }
        });
        modalInstance.result.then(function (application) {
          // create a new application from the given name and description.
          repositoryService.create({}, angular.toJson(application), function (response) {
            $scope.searchConfig.service.search();
          });
        });
      };
    }]);
});