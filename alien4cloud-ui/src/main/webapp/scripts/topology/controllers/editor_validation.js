// Editor validation controller.
define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-topology-editor', ['a4c-common']).controller('TopologyValidationCtrl',
    ['$scope', 'topologyServices', 'tasksProcessor', '$alresource',
    function($scope, topologyServices, tasksProcessor, $alresource) {

      var editorIsTopologyValid = $alresource('rest/latest/editor/:topologyId/isvalid');
      var isCurrentTopologyValid = function() {
        if (_.undefined($scope.topology)) {
          topologyServices.dao.get({
            topologyId: $scope.topologyId
          }, function(successResult) {
            if (_.undefined(successResult.error)){
              $scope.refreshTopology(successResult.data);
            }
          });
        } else if($scope.topology.operations && $scope.topology.operations.length === 0 || $scope.topology.lastOperationIndex===-1) {
          // nothing to check
          return;
        }

        editorIsTopologyValid.create({
          topologyId: $scope.topologyId
        }, null, function(result) {
          if(_.undefined(result.error)) {
            $scope.validCurrentTopologyDTO = result.data;
            tasksProcessor.processAll($scope.validCurrentTopologyDTO);
          }
        });
      };
      isCurrentTopologyValid();

      var isTopologyValid = function isTopologyValid() {
        return topologyServices.isValid({
          topologyId: $scope.topologyId
        }, function(result) {
          $scope.validTopologyDTO = result.data;
          tasksProcessor.processAll($scope.validTopologyDTO);
        });
      };
      isTopologyValid();

      $scope.$on('topologyRefreshedEvent', function() {
        isTopologyValid();
      });
    }]
  );
});
