define(function(require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');
  var angular = require('angular');
  require('scripts/orchestrators/controllers/orchestrator_location_resource_template');
  require('scripts/orchestrators/directives/orchestrator_location_resource_template');
  require('scripts/applications/services/deployment_topology_processor.js');

  states.state('applications.detail.deployment.match', {
    url: '/match',
    resolve: {
      substitutionContext: ['application', 'appEnvironments', 'deploymentTopologyServices', 'deploymentContext', 'deploymentTopologyProcessor',
        function(application, appEnvironments, deploymentTopologyServices, deploymentContext, deploymentTopologyProcessor) {
          return deploymentTopologyServices.getAvailableSubstitutions({
            appId: application.data.id,
            envId: deploymentContext.selectedEnvironment.id
          }).$promise.then(function(response) {
              deploymentTopologyProcessor.processSubstitutionResources(response.data);
              return response.data;
            }
          );
        }]
    },
    templateUrl: 'views/applications/application_deployment_match.html',
    controller: 'ApplicationDeploymentMatchCtrl',
    menu: {
      id: 'am.applications.detail.deployment.match',
      state: 'applications.detail.deployment.match',
      key: 'APPLICATIONS.DEPLOYMENT.MATCHING',
      roles: ['APPLICATION_MANAGER', 'APPLICATION_DEPLOYER'],
      priority: 200
    }
  });

  modules.get('a4c-applications').controller('ApplicationDeploymentMatchCtrl',
    ['$scope', 'nodeTemplateService', 'substitutionContext', 'deploymentTopologyServices', 'deploymentTopologyProcessor',
      function($scope, nodeTemplateService, substitutionContext, deploymentTopologyServices, deploymentTopologyProcessor) {
        $scope.substitutionContext = substitutionContext;
        $scope.getIcon = function(template) {
          if (!_.isEmpty($scope.substitutionContext.substitutionTypes.nodeTypes)) {
            var templateType = $scope.substitutionContext.substitutionTypes.nodeTypes[template.template.type];
            if (!_.isEmpty(templateType)) {
              return nodeTemplateService.getNodeTypeIcon(templateType);
            }
          }
        };

        $scope.getSubstitutedTemplate = function(nodeName) {
          return $scope.deploymentContext.deploymentTopologyDTO.topology.substitutedNodes[nodeName];
        };

        $scope.selectTemplate = function(nodeName, template) {
          $scope.selectedNodeName = nodeName;
          var substitutedNode = $scope.getSubstitutedTemplate(nodeName);
          if (substitutedNode.id !== template.id) {
            deploymentTopologyServices.updateSubstitution({
              appId: $scope.application.id,
              envId: $scope.deploymentContext.selectedEnvironment.id,
              nodeId: nodeName,
              locationResourceTemplateId: template.id
            }, undefined, function(response) {
              deploymentTopologyProcessor.process(response.data);
              $scope.deploymentContext.deploymentTopologyDTO = response.data;
              $scope.selectedResourceTemplate = $scope.getSubstitutedTemplate(nodeName);
            });
          } else {
            $scope.selectedResourceTemplate = substitutedNode;
          }
        };

        $scope.updateSubstitutionProperty = function(propertyName, propertyValue) {
          return deploymentTopologyServices.updateSubstitutionProperty({
            appId: $scope.application.id,
            envId: $scope.deploymentContext.selectedEnvironment.id,
            nodeId: $scope.selectedNodeName
          }, angular.toJson({
            propertyName: propertyName,
            propertyValue: propertyValue
          })).$promise;
        };

        $scope.updateSubstitutionCapabilityProperty = function(capabilityName, propertyName, propertyValue) {
          return deploymentTopologyServices.updateSubstitutionCapabilityProperty({
            appId: $scope.application.id,
            envId: $scope.deploymentContext.selectedEnvironment.id,
            nodeId: $scope.selectedNodeName,
            capabilityName: capabilityName
          }, angular.toJson({
            propertyName: propertyName,
            propertyValue: propertyValue
          })).$promise;
        };
      }
    ]); //controller
}); //Define
