define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');
  var angular = require('angular');

  require('scripts/orchestrators/controllers/orchestrator_location_modifiers_new');

  states.state('admin.orchestrators.details.locations.modifiers', {
    url: '/modifiers',
    templateUrl: 'views/orchestrators/orchestrator_location_modifiers.html',
    controller: 'OrchestratorLocationModifiersCtrl',
    menu: {
      id: 'menu.orchestrators.locations.modifiers',
      state: 'admin.orchestrators.details.locations.modifiers',
      key: 'ORCHESTRATORS.LOCATIONS.MODIFIER.MODIFIER',
      icon: 'fa fa-random',
      priority: 400,
      active: true
    }
  });

  modules.get('a4c-orchestrators').controller('OrchestratorLocationModifiersCtrl', ['$scope', '$http', '$alresource', '$uibModal',
    function($scope, $http, $alresource, $uibModal) {
      var locationModifierResource = $alresource('rest/latest/orchestrators/:orchestratorId/locations/:locationId/modifiers/:modifierIndex');
      var locationMoveModifierResource = $alresource('rest/latest/orchestrators/:orchestratorId/locations/:locationId/modifiers/from/:fromIndex/to/:toIndex');

      $http.get('rest/latest/plugincomponents?type=ITopologyModifier').then(function(response) {
        if (_.defined(response.data.data)) {
          $scope.availableModifiers = response.data.data;
        }
      });

      function updateList() {
        locationModifierResource.get({orchestratorId: $scope.context.orchestrator.id, locationId: $scope.context.location.id }, function(response) {
          $scope.locationModifiers = response.data;
        });
      }

      $scope.$watch('context.location.id',function() {
        updateList();
      });

      $scope.deleteModifier = function(index) {
        locationModifierResource.remove(
          { orchestratorId: $scope.context.orchestrator.id, locationId: $scope.context.location.id, modifierIndex: index },
          function(){
            updateList();
          });
      };

      $scope.moveModifier = function(from, to) {
          locationMoveModifierResource.update(
          { orchestratorId: $scope.context.orchestrator.id, locationId: $scope.context.location.id, fromIndex: from, toIndex: to }, undefined,
          function(){
            updateList();
          });
      };

      $scope.addModifier = function(dragData) {
        var source = angular.fromJson(dragData.source);
        if (!source) {
          return;
        }

        var modalInstance = $uibModal.open({
          templateUrl: 'views/orchestrators/orchestrator_location_modifiers_new.html',
          controller: 'LocationModifierNewController',
          resolve: {
            modifierReference: function() {
              return {
                pluginId: source.pluginId,
                beanName: source.componentDescriptor.beanName,
                phase: 'post-location-match'
              };
            }
          }
        });

        modalInstance.result.then(function(modifierReference) {
          locationModifierResource.create(
            {orchestratorId: $scope.context.orchestrator.id, locationId: $scope.context.location.id },
            angular.toJson(modifierReference), function() {
              // add modifier to the location
              updateList();
            });
        });
      };
    }
  ]);
}); // define
