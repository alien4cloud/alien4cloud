define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');

  states.state('admin.orchestrators.details.artifacts', {
    url: '/artifacts',
    templateUrl: 'views/orchestrators/orchestrator_artifacts.html',
    controller: 'OrchestratorArtifactsCtrl',
    menu: {
      id: 'menu.orchestrators.artifacts',
      state: 'admin.orchestrators.details.artifacts',
      key: 'ORCHESTRATORS.NAV.ARTIFACTS',
      icon: 'fa fa-file-text-o',
      priority: 500
    }
  });

  modules.get('a4c-orchestrators').controller('OrchestratorArtifactsCtrl',
    ['$scope' , '$state', '$resource', 'orchestrator', 'breadcrumbsService', '$translate',
    function($scope, $state, $resource, orchestrator, breadcrumbsService, $translate) {

      breadcrumbsService.putConfig({
        state: 'admin.orchestrators.details.artifacts',
        text: function() {
          return $translate.instant('ORCHESTRATORS.NAV.ARTIFACTS');
        }
      });

      $scope.artifactTypes = [];
      $resource('rest/latest/orchestrators/'+orchestrator.id+'/artifacts-support').get(
        {},
        function(result){
          if(!_.isEmpty(result.data)){
            $scope.artifactTypes = result.data;
          }
        },
        function(){}); // error handler
    }
  ]); // controller

}); // define
