define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');

  require('scripts/_ref/admin/services/plugin_services');
  require('scripts/common/services/search_service_factory');
  require('scripts/common/directives/upload');
  require('scripts/common/directives/delete_confirm');
  require('scripts/common/directives/generic_form');
  require('scripts/common/services/formdescriptor_services');

  // register state for user management.
  states.state('admin.plugins', {
    url: '/plugins',
    templateUrl: 'views/_ref/admin/admin_plugins_list.html',
    controller: 'PluginCtrl',
    menu: {
      id: 'am.admin.plugins',
      state: 'admin.plugins',
      key: 'NAVADMIN.MENU_PLUGINS',
      icon: 'fa fa-puzzle-piece',
      priority: 200
    }
  });

  require('scripts/common/directives/pagination');

  var ConfigPluginCtrl = ['$scope', '$uibModalInstance', '$resource', 'formDescriptorServices',
    function($scope, $uibModalInstance, $resource, formDescriptorServices) {
      //descriptor of the config
      formDescriptorServices.getForPluginConfig({
        pluginId: $scope.toConfigPluginId
      }, function(result) {
        $scope.pluginConfigDefinition = result.data;
      });

      $scope.saveConfiguration = function(pluginConfig) {
        $uibModalInstance.close(pluginConfig);
      };

      $scope.cancel = function() {
        $uibModalInstance.dismiss('cancel');
      };
    }
  ];

  modules.get('a4c-plugins', ['ngResource', 'ui.bootstrap', 'pascalprecht.translate', 'toaster', 'a4c-search', 'a4c-common'])
    .controller('PluginCtrl', ['$scope', '$resource', 'searchServiceFactory', '$uibModal', 'pluginServices', '$translate', 'toaster',
    function($scope, $resource, searchServiceFactory, $uibModal, pluginServices, $translate, toaster) {
      $scope.query = '';
      $scope.onSearchCompleted = function(searchResult) {
        $scope.data = searchResult.data;
      };
      $scope.searchService = searchServiceFactory('rest/latest/plugins', true, $scope, 20);

      $scope.search = function() {
        $scope.searchService.search();
      };

      //first load
      $scope.search();

      var enableResource = $resource('rest/latest/plugins/:pluginId/enable', {}, {
        'enable': {
          method: 'GET',
          params: {
            pluginId: '@pluginId'
          }
        }
      });

      var disableResource = $resource('rest/latest/plugins/:pluginId/disable', {}, {
        'disable': {
          method: 'GET',
          params: {
            pluginId: '@pluginId'
          }
        }
      });

      // Prepare result html for toaster message
      function buildResultList(resultObject) {
        var baseResponse = $translate.instant('PLUGINS.ERRORS.' + resultObject.error.code);
        var resourtceList = baseResponse + ' : <ul>';
        resultObject.data.forEach(function getResource(resource) {
          resourtceList += '<li>';
          resourtceList += resource.resourceType + ' : ' + resource.resourceName;
          resourtceList += '</li>';
        });
        return resourtceList;
      }

      function handleError(result) {
        var resultHtml = buildResultList(result);
        // toaster message
        toaster.pop('error', $translate.instant('PLUGINS.ERRORS.' + result.error.code + '_TITLE'), resultHtml, 4000, 'trustedHtml', null);
      }

      function enablePlugin(plugin) {
        plugin.pending=true;
        enableResource.enable([], {
          pluginId: plugin.id
        }, function(result) {
          plugin.pending=false;
          if (!result.error) {
            plugin.enabled = !plugin.enabled;
          }
        });
      }

      function disablePlugin(plugin) {
        plugin.pending=true;
        disableResource.disable([], {
          pluginId: plugin.id
        }, function(result) {
          plugin.pending=false;
          if (!result.error) {
            plugin.enabled = !plugin.enabled;
          } else {
            handleError(result);
          }
        });
      }

      $scope.toggleState = function(plugin) {
        switch (plugin.enabled) {
          case true:
            disablePlugin(plugin);
            break;
          case false:
            enablePlugin(plugin);
            break;
          default:
            return;
        }
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

      $scope.openConfiguration = function(pluginId) {
        $scope.toConfigPluginId = pluginId;

        var doHandleModal = function() {
          var modalInstance = $uibModal.open({
            templateUrl: 'views/_ref/admin/admin_plugins_configuration.html',
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
});
