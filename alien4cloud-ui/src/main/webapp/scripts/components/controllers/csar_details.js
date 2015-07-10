define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');

  require('scripts/components/services/csar');
  require('scripts/common/services/websocket_services');
  require('scripts/deployment/services/deployment_services');

  states.state('components.csars.csardetail', {
    url: '/detail/:csarId',
    templateUrl: 'views/components/csar_details.html',
    controller: 'CsarDetailsCtrl'
  });

  modules.get('a4c-components', ['ui.router', 'ui.bootstrap', 'a4c-deployment']).controller(
    'CsarDetailsCtrl', ['$scope', '$stateParams', '$state', 'csarService', 'deploymentServices', 'webSocketServices', '$translate', 'toaster',
    function($scope, $stateParams, $state, csarService, deploymentServices, webSocketServices, $translate, toaster) {
      /* Retrieve CSAR to display */
      $scope.csarId = $stateParams.csarId;
      $scope.refreshDetails = function() {
        csarService.getAndDeleteCsar.get({
          csarId: $scope.csarId
        }, function(successResult) {
          $scope.csar = successResult.data;
          refreshDeploymentStatus();
        });
      };

      var onStatusChange = function(event) {
        $scope.deploymentStatus = JSON.parse(event.body).deploymentStatus;
        $scope.$apply();
      };

      $scope.$on('$destroy', function() {
        // Unsubscribe
        unsubscribeFromDeploymentStatusTopic();
      });

      var subscribeToDeploymentStatusTopic = function() {
        if (!webSocketServices.isTopicSubscribed($scope.deploymentEventsTopic)) {
          webSocketServices.subscribe($scope.deploymentEventsTopic, onStatusChange);
        }
      };

      var unsubscribeFromDeploymentStatusTopic = function() {
        if (_.defined($scope.deploymentEventsTopic) && webSocketServices.isTopicSubscribed($scope.deploymentEventsTopic)) {
          webSocketServices.unSubscribe($scope.deploymentEventsTopic);
        }
      };

      var refreshDeploymentStatus = function() {
        csarService.getActiveDeployment.get({
          csarId: $scope.csarId
        }, undefined, function(success) {
          if (_.defined(success.data)) {
            var deployment = success.data;
            $scope.deploymentId = deployment.id;
            $scope.deploymentEventsTopic = '/topic/deployment-events/' + $scope.deploymentId + '/paasdeploymentstatusmonitorevent';
            // Subscribe to listen to event on status change from server side
            unsubscribeFromDeploymentStatusTopic();
            subscribeToDeploymentStatusTopic();
            refreshStatus();
          } else {
            $scope.deploymentStatus = 'UNDEPLOYED';
          }
        });
      };

      var refreshStatus = function() {
        deploymentServices.getStatus({
          deploymentId: $scope.deploymentId
        }, function(successResult) {
          $scope.deploymentStatus = successResult.data;
        });
      };

      $scope.remove = function(csarId) {
        csarService.getAndDeleteCsar.remove({
          csarId: csarId
        }, function(result) {
          var errorMessage = csarService.builtErrorResultList(result);
          if (errorMessage) {
            var title = $translate('CSAR.ERRORS.' + result.error.code + '_TITLE');
            toaster.pop('error', title, errorMessage, 4000, 'trustedHtml', null);
          } else {
            $state.go('components.csars.list');
          }
        });
      };

      $scope.undeploy = function() {
        $scope.isUnDeploying = true;
        deploymentServices.undeploy({
          deploymentId: $scope.deploymentId
        }, function() {
          $scope.deploymentStatus = 'UNDEPLOYMENT_IN_PROGRESS';
          $scope.isUnDeploying = false;
        });
      };

      // init details page
      $scope.refreshDetails();
    }
  ]); // controller
});// define
