// Editor validation controller.
define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-topology-editor', ['a4c-common']).controller('TopologyValidationCtrl',
    ['$scope', 'topologyServices', 'tasksProcessor',
    function($scope, topologyServices, tasksProcessor) {

      var isTopologyValid = function isTopologyValid(topologyId) {
        return topologyServices.isValid({
          topologyId: topologyId
        }, function(result) {
          $scope.validTopologyDTO = result.data;
          tasksProcessor.processAll($scope.validTopologyDTO);
        });
      };
      isTopologyValid($scope.topologyId);
    }]
  );
});
