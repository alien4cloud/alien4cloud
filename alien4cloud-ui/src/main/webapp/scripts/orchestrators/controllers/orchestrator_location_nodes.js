define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

  require('scripts/orchestrators/controllers/orchestrator_location_resources');
  require('scripts/orchestrators/directives/orchestrator_location_resources');

  states.state('admin.orchestrators.details.locations.nodes', {
    url: '/nodes',
    templateUrl: 'views/orchestrators/orchestrator_location_nodes.html',
    controller: 'OrchestratorNodesCtrl',
    menu: {
      id: 'menu.orchestrators.locations.nodes',
      state: 'admin.orchestrators.details.locations.nodes',
      key: 'ORCHESTRATORS.LOCATIONS.ON_DEMAND_RESOURCES',
      icon: 'fa fa-cubes',
      priority: 200
    }
  });
  
  modules.get('a4c-orchestrators').controller('OrchestratorNodesCtrl', ['$scope', '$resource', 'locationResourcesProcessor',
    function($scope, $resource, locationResourcesProcessor) {

      function removeGeneratedResources() {
          _.remove($scope.context.locationResources.nodeTemplates, function(locationResource){
            return locationResource.generated;
          });
        }
      
      $scope.autoConfigureResources = function(){
        console.log('autoconfiguring');
        $scope.autoConfiguring = true
        $resource('rest/orchestrators/'+$scope.context.orchestrator.id+'/locations/'+$scope.context.location.id+'/resources/auto-configure').get({}, 
            function(result){
              if(_.undefined($scope.context.locationResources.nodeTemplates)){
                $scope.context.locationResources.nodeTemplates = [];
              }
              removeGeneratedResources();
              if(!_.isEmpty(result.data)){
                locationResourcesProcessor.processLocationResourceTemplates(result.data)
                $scope.context.locationResources.nodeTemplates = $scope.context.locationResources.nodeTemplates.concat(result.data);
              }
              $scope.autoConfiguring = false;
        }, function(){
          $scope.autoConfiguring=false;
        });
      };
      
      
      
    }
  ]);
}); // define
