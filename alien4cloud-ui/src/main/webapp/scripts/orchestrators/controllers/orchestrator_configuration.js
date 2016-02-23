define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

  require('scripts/orchestrators/services/orchestrator_configuration_service');

  states.state('admin.orchestrators.details.configuration', {
    url: '/configuration',
    templateUrl: 'views/orchestrators/orchestrator_configuration.html',
    controller: 'OrchestratorConfigurationCtrl',
    menu: {
      id: 'menu.orchestrators.configuration',
      state: 'admin.orchestrators.details.configuration',
      key: 'ORCHESTRATORS.NAV.CONFIGURATION',
      icon: 'fa fa-wrench',
      priority: 300
    }
  });

  modules.get('a4c-orchestrators').controller('OrchestratorConfigurationCtrl',
    ['$scope', '$http', '$translate', '$q', 'orchestrator', 'orchestratorConfigurationService', 'orchestratorService',
    function($scope, $http, $translate, $q, orchestrator, orchestratorConfigurationService, orchestratorService) {
      $scope.orchestrator = orchestrator;
      $scope.lock = true;

      $scope.toggleLock = function() {
        $scope.lock = ($scope.lock) ? false : true;
      };

      orchestratorConfigurationService.get({orchestratorId: orchestrator.id},
        function(response) {
          if (_.defined(response.data)) {
            $scope.configuration = response.data.configuration;
          }
        }
      );

      // get the configuration for the orchestrator.
      $http.get('rest/formdescriptor/orchestratorConfig/' + orchestrator.id).success(function(result) {
        $scope.configurationDefinition = result.data;
      });

      $scope.saveConfiguration = function(newConfiguration) {
        if (orchestrator.state === 'CONNECTED') {
          $scope.toggleLock();
        }
        return orchestratorConfigurationService.update({
          orchestratorId: orchestrator.id
        }, angular.toJson(newConfiguration), function success(response) {
          $scope.configuration = newConfiguration;
          if (_.defined(response.error)) {
            var errorsHandle = $q.defer();
            return errorsHandle.resolve(response.error);
          }
        }).$promise;
      };

      $scope.updateDeploymentNamePattern = function(newPattern) {
        if (newPattern !== orchestrator.deploymentNamePattern) {
          var request = {deploymentNamePattern:newPattern};
          return orchestratorService.update({orchestratorId: orchestrator.id}, angular.toJson(request)).$promise.then(
            function() {}, // Success
            function(errorResponse) {
              return $translate('ERRORS.' + errorResponse.data.error.code);
            });
        }
      };

    }
  ]); // controller
}); // define
