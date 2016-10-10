// define the rest api elements to work with topology edition.
define(function (require) {
  'use strict';
  var modules = require('modules');
  var _ = require('lodash');

  var RecoveryChoiceCtrl = ['$scope', '$modalInstance', 'recoveryOperation',
    function($scope, $modalInstance, recoveryOperation) {
      $scope.choice = {};
      $scope.recoveryOperation = recoveryOperation;
      $scope.choose = function(action) {
        $modalInstance.close(action);
      };

      $scope.cancel = function() {
        $modalInstance.dismiss('cancel');
      };
    }];

  modules.get('a4c-topology-editor', ['ngResource']).factory('topologyRecoveryServices', ['$alresource', '$modal', '$q', 'toaster', '$translate', '$state',
    function($alresource, $modal, $q, toaster, $translate, $state) {

      var editorRecoverResource = $alresource('rest/latest/editor/:topologyId/recover');
      var editorResetResource = $alresource('rest/latest/editor/:topologyId/reset');


      var handleRecoveryChoice = function( choice, topologyId, lastOperationId){
        switch (choice) {
          case 1:
            return editorRecoverResource.update({
              topologyId: topologyId,
              lastOperationId: lastOperationId
            }, undefined).$promise.then(function(topoDTO){
              return topoDTO;
            });
          case 2:
            return editorResetResource.update({
              topologyId: topologyId,
              lastOperationId: lastOperationId
            }, undefined).$promise.then(function(topoDTO){
              return topoDTO;
            });
          default:
            return null;
        }

      };

      /** handle Modal form for recovery choices */
      var openRecoveryChoiceModal = function (recoveryOperation) {
        var deferred = $q.defer();
        var modalInstance = $modal.open({
          templateUrl: 'views/topology/topology_recovery_modal.html',
          controller: RecoveryChoiceCtrl,
          resolve: {
            recoveryOperation: function () {
              return recoveryOperation;
            }
          }
        });
        modalInstance.result.then(function (recoveryChoice) {
          deferred.resolve(recoveryChoice);
        }, function(){
          //case the modal was dissmissed
          //this means do nothing with the topology
          deferred.resolve(-1);
        });
        return deferred.promise;
      };

      var handleTopologyRecovery = function(recoveryOperation, topologyId, lastOperationId) {
        var updatedDependencies = recoveryOperation.updatedDependencies;

        //if no updated dependencies, then do nothing
        //should never happen
        if(_.isEmpty(updatedDependencies)){
          return null;
        }

        return openRecoveryChoiceModal(recoveryOperation).then(function(choice){
          var result = handleRecoveryChoice(choice, topologyId, lastOperationId);
          if(result !== null){
            return result.then(function(result){
              toaster.pop('success', $translate.instant('APPLICATIONS.TOPOLOGY.RECOVERY.TITLE'), $translate.instant('APPLICATIONS.TOPOLOGY.RECOVERY.SUCCESS_MSGE'), 4000, 'trustedHtml', null);
              return result;
            });
          }else {
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
