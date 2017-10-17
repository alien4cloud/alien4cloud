// Editor validation controller.
define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-topology-editor', ['a4c-common']).controller('TopologyValidationCtrl',
    ['$scope', 'topologyServices', 'tasksProcessor', '$alresource', 'topoEditDisplay',
    function($scope, topologyServices, tasksProcessor, $alresource, topoEditDisplay) {
      topoEditDisplay($scope, '#topology_validation');

      var editedTopologyValidatorResource = $alresource('rest/latest/editor/:topologyId/isvalid');

      function updateValidationDtos() {
        //validate topology beeing edited
        editedTopologyValidatorResource.create({
          topologyId: $scope.topologyId
        }, null, function(result) {
          if(_.undefined(result.error)) {
            $scope.editedTopologyValidationDTO = result.data;
            tasksProcessor.processAll($scope.editedTopologyValidationDTO);
          }
        });

        //validate las saved topology
        topologyServices.isValid({
          topologyId: $scope.topologyId
        }, function(result) {
          $scope.lastSavedTopologyValidationDto = result.data;
          tasksProcessor.processAll($scope.lastSavedTopologyValidationDto);
        });
      }

      updateValidationDtos();


      $scope.$on('topologyRefreshedEvent', function(event, data) {
        // no need to do this if it is the initial load of the topology, since validation is done on controller loaded
        if(!data.initial){
          updateValidationDtos();
        }
      });
    }]
  );
});
