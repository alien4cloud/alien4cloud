define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');

  require('scripts/admin/server/metrics_service');

  // register the state to access the metrics
  states.state('maintenance', {
    url: '/maintenance',
    templateUrl: 'views/common/maintenance.html',
    controller: 'MantenanceCtrl'
  });

  modules.get('alien4cloud-admin').controller('MantenanceCtrl', [
    '$scope', '$uibModal', '$state', '$translate', '$alresource', 'authService',
    function ($scope, $uibModal, $state, $translate, $alresource, authService) {
      $scope.isAdmin = authService.hasRole('ADMIN');
      $scope.progress = 0;
      var maintenanceService = $alresource('rest/latest/maintenance/');
      function updateState() {
        $scope.maintenanceState = maintenanceService.get();
        $scope.maintenanceState.$promise.then(function() {
          $scope.progress = $scope.maintenanceState.data.progressPercent;
        });
      }
      updateState();
      $scope.disable = function() {
        var modalInstance = $uibModal.open({
          templateUrl: 'views/common/confirm_modal.html',
          controller: 'ConfirmModalCtrl',
          resolve: {
            title: function() {
              return 'SERVER.MAINTENANCE.DISABLE';
            },
            content: function() {
              return $translate('SERVER.MAINTENANCE.DISABLE_CONFIRM');
            }
          }
        });
        modalInstance.result.then(function () {
          maintenanceService.delete(undefined, undefined, function() {
            $state.go('admin.server');
          });
        });
      };

      $scope.update = function() {
        console.log('Update', $scope.message, $scope.progress);
        maintenanceService.update(undefined, angular.toJson( {
          message: $scope.message,
          progressPercentage: $scope.progress
        }), function() {
          updateState();
        });
      };
    }
  ]);
});
