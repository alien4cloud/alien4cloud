'use strict';

angular.module('alienUiApp').controller(
  'CsarDetailsCtrl', ['$scope', '$stateParams', '$state', 'csarService', 'suggestionServices', 'formDescriptorServices', 'deploymentServices', 'webSocketServices', '$resource', 'topologyServices',
    function($scope, $stateParams, $state, csarService, suggestionServices, formDescriptorServices, deploymentServices, webSocketServices, $resource, topologyServices) {
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
        if (UTILS.isDefinedAndNotNull($scope.deploymentEventsTopic) && webSocketServices.isTopicSubscribed($scope.deploymentEventsTopic)) {
          webSocketServices.unSubscribe($scope.deploymentEventsTopic);
        }
      };

      var refreshDeploymentStatus = function() {
        csarService.getActiveDeployment.get({
          csarId: $scope.csarId
        }, undefined, function(success) {
          if (UTILS.isDefinedAndNotNull(success.data)) {
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

      $scope.goToNodeTypeDetail = function(nodeTypeId) {
        $state.go('components.csars.csardetailnode', {
          csarId: $scope.csarId,
          nodeTypeId: nodeTypeId
        });
      };

      $scope.remove = function(csarId) {
        csarService.getAndDeleteCsar.remove({
          csarId: csarId
        }, function() {
          $state.go('components.csars.list');
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

      formDescriptorServices.getNodeTypeFormDescriptor().then(function(result) {
        $scope.objectDefinition = result;
      });

      $scope.suggest = function(searchConfiguration, text) {
        return suggestionServices.getSuggestions(searchConfiguration._index, searchConfiguration._type, searchConfiguration._path, text);
      };

      $scope.saveNodeType = function(nodeType) {
        csarService.createNodeType.upload({
          csarId: $scope.csarId
        }, angular.toJson(nodeType), function() {
          $scope.refreshDetails();
        });
      };

      $scope.formTitle = 'CSAR.DETAILS.COMPONENT_FORM_TITLE';
    }
  ]);
