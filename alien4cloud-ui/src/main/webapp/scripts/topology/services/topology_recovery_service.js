// define the rest api elements to work with topology edition.
define(function (require) {
  'use strict';
  var modules = require('modules');
  var _ = require('lodash');

  var RecoveryChoiceCtrl = ['$scope', '$uibModalInstance', 'recoveryOperation', 'errors',
    function ($scope, $uibModalInstance, recoveryOperation, errors) {
      $scope.choice = {};
      $scope.recoveryOperation = recoveryOperation;
      $scope.errors = errors;
      $scope.choose = function (action) {
        $uibModalInstance.close(action);
      };

      $scope.cancel = function () {
        $uibModalInstance.dismiss('cancel');
      };
    }];

  modules.get('a4c-topology-editor', ['ngResource']).factory('topologyRecoveryServices', ['$alresource', '$uibModal', '$q', 'toaster', '$translate', '$state',
    function ($alresource, $uibModal, $q, toaster, $translate, $state) {

      var editorRecoverResource = $alresource('rest/latest/editor/:topologyId/recover');
      var editorResetResource = $alresource('rest/latest/editor/:topologyId/reset');

      var handleRecoveryChoice = function (choice, topologyId, lastOperationId) {
        switch (choice) {
          case 1:
            return editorRecoverResource.update({
              topologyId: topologyId,
              lastOperationId: lastOperationId
            }, undefined).$promise.then(function (topoDTO) {
              return topoDTO;
            });
          case 2:
            return editorResetResource.update({
              topologyId: topologyId,
              lastOperationId: lastOperationId
            }, undefined).$promise.then(function (topoDTO) {
              return topoDTO;
            });
          default:
            return null;
        }

      };

      /** handle Modal form for recovery choices */
      var openRecoveryChoiceModal = function (recoveryOperation, errors) {
        var deferred = $q.defer();
        var modalInstance = $uibModal.open({
          templateUrl: 'views/topology/topology_recovery_modal.html',
          controller: RecoveryChoiceCtrl,
          resolve: {
            recoveryOperation: function () {
              return recoveryOperation;
            },
            errors: function () {
              return errors;
            }
          }
        });
        modalInstance.result.then(function (recoveryChoice) {
          deferred.resolve(recoveryChoice);
        }, function () {
          //case the modal was dissmissed
          //this means do nothing with the topology
          deferred.resolve(-1);
        });
        return deferred.promise;
      };

      var isReleasedTopology = function (topologyId) {
        var indexOfTwoPoint = topologyId.indexOf(':');
        if (indexOfTwoPoint >= 0) {
          var version = topologyId.substring(indexOfTwoPoint + 1, topologyId.length);
          return version.toUpperCase().indexOf('SNAPSHOT') < 0;
        } else {
          return false;
        }
      };

      var handleTopologyRecovery = function (recoveryOperation, topologyId, lastOperationId) {
        var errors;
        if (isReleasedTopology(topologyId)) {
          errors = ['RELEASED'];
        }
        var updatedDependencies = recoveryOperation.updatedDependencies;

        //if no updated dependencies, then do nothing
        //should never happen
        if (_.isEmpty(updatedDependencies)) {
          return null;
        }

        return openRecoveryChoiceModal(recoveryOperation, errors).then(function (choice) {
          var result = handleRecoveryChoice(choice, topologyId, lastOperationId);
          if (result !== null) {
            return result.then(function (result) {
              toaster.pop('success', $translate.instant('APPLICATIONS.TOPOLOGY.RECOVERY.TITLE'), $translate.instant('APPLICATIONS.TOPOLOGY.RECOVERY.SUCCESS_MSGE'), 4000, 'trustedHtml', null);
              return result;
            });
          } else {
            //redirect to the application list view
            //TODO shoud alos be able to go to template list, that is if the recovery was triggered on a topology template
            $state.go('applications.list');
          }
        });
      };

      return {
        'handleTopologyRecovery': handleTopologyRecovery
      };
    }
  ]);
}); // define
