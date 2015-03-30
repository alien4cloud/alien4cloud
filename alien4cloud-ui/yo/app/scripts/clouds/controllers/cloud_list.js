'use strict';

angular.module('alienUiApp').controller('CloudListController', ['$scope', '$resource', '$state', 'searchServiceFactory', '$modal', 'cloudServices',
  function($scope, $resource, $state, searchServiceFactory, $modal, cloudServices) {
    $scope.query = '';
    $scope.onSearchCompleted = function(searchResult) {
      $scope.data = searchResult.data;
    };
    $scope.searchService = searchServiceFactory('rest/clouds/search', true, $scope, 20);

    $scope.search = function() {
      $scope.searchService.search();
    };

    // first load
    $scope.search();

    /** handle Modal form for user creation */
    $scope.openNewModal = function() {
      var modalInstance = $modal.open({
        templateUrl: 'views/clouds/new_cloud.html',
        controller: 'NewCloudController'
      });

      modalInstance.result.then(function(newCloud) {
        var cloud = {
          name: newCloud.name,
          paasPluginId: newCloud.paaSProvider.pluginId,
          paasPluginBean: newCloud.paaSProvider.componentDescriptor.beanName,
          paasProviderName: newCloud.paaSProvider.componentDescriptor.name
        };

        cloudServices.create([], angular.toJson(cloud), function() {
          $scope.searchService.search();
        });
      });
    };

    $scope.openCloud = function(id) {
      $state.go('admin.clouds.detail', {id : id});
    };

    $scope.cloneCloud = function(id) {
      cloudServices.cloneCloud({
        id: id
      }, {}, function(response) {
        if (UTILS.isDefinedAndNotNull(response.data)) {
          $scope.openCloud(response.data);
        }
      });
    };
  }
]);
