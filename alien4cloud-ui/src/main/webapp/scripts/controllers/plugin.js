'use strict';

var ConfigPluginCtrl = ['$scope', '$modalInstance', '$resource', 'formDescriptorServices',
  function($scope, $modalInstance, $resource, formDescriptorServices) {
    //descriptor of the config
    formDescriptorServices.getForPluginConfig({
      pluginId: $scope.toConfigPluginId
    }, function(result) {
      $scope.pluginConfigDefinition = result.data;
    });

    $scope.saveConfiguration = function(pluginConfig) {
      $modalInstance.close(pluginConfig);
    };

    $scope.cancel = function() {
      $modalInstance.dismiss('cancel');
    };

  }
];

angular.module('alienUiApp').controller('PluginCtrl', ['$scope', '$resource', 'searchServiceFactory', '$modal', 'pluginServices', '$translate', 'toaster',
  function($scope, $resource, searchServiceFactory, $modal, pluginServices, $translate, toaster) {
    $scope.query = '';
    $scope.onSearchCompleted = function(searchResult) {
      $scope.data = searchResult.data;
    };
    $scope.searchService = searchServiceFactory('rest/plugin/search', false, $scope, 20);

    $scope.search = function() {
      $scope.searchService.search();
    };

    //first load
    $scope.search();

    var enableResource = $resource('rest/plugin/:pluginId/enable', {}, {
      'enable': {
        method: 'GET',
        params: {
          pluginId: '@pluginId'
        }
      }
    });

    var disableResource = $resource('rest/plugin/:pluginId/disable', {}, {
      'disable': {
        method: 'GET',
        params: {
          pluginId: '@pluginId'
        }
      }
    });

    $scope.enable = function(pluginId) {
      enableResource.enable([], {
        pluginId: pluginId
      }, function(result) {
        if (!result.error) {
          $scope.searchService.search();
        }
      });
    };

    $scope.disable = function(pluginId) {
      disableResource.disable([], {
        pluginId: pluginId
      }, function(result) {
        if (!result.error) {
          $scope.searchService.search();
        } else {
          handleError(result);
        }
      });
    };

    $scope.remove = function(pluginId) {
      pluginServices.remove({
        pluginId: pluginId
      }, function(result) {
        if (!result.error) {
          $scope.searchService.search();
        } else {
          handleError(result);
        }
      });
    };

    function handleError(result) {
      var resultHtml = builtResultList(result);
      // toaster message
      toaster.pop('error', $translate('PLUGINS.ERRORS.' + result.error.code + '_TITLE'), resultHtml, 4000, 'trustedHtml', null);
    }

    // Prepare result html for toaster message
    function builtResultList(resultObject) {
      var baseResponse = $translate('PLUGINS.ERRORS.' + resultObject.error.code);
      var resourtceList = baseResponse + ' : <ul>';
      resultObject.data.forEach(function getResource(resource) {
        resourtceList += '<li>';
        resourtceList += resource.resourceType + ' : ' + resource.resourceName;
        resourtceList += '</li>';
      });
      return resourtceList;
    }

    $scope.openConfiguration = function(pluginId) {
      $scope.toConfigPluginId = pluginId;

      var doHandleModal = function() {
        var modalInstance = $modal.open({
          templateUrl: 'views/plugin_configuration.html',
          controller: ConfigPluginCtrl,
          windowClass: 'searchModal',
          scope: $scope
        });

        modalInstance.result.then(function(pluginConfig) {
          pluginServices.config.save({
            pluginId: $scope.toConfigPluginId
          }, angular.toJson(pluginConfig), function() {
          });
        });
      };

      //if a config already exists, then load it before
      pluginServices.config.get({
        pluginId: $scope.toConfigPluginId
      }, function(result) {
        if (result.data) {
          $scope.pluginConfig = result.data;
        }
        doHandleModal();
      });

    };

  }
]);
