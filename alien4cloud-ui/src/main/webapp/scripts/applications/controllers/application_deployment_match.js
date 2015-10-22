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
    templateUrl: 'views/applications/application_deployment_match.html',
    controller: 'ApplicationDeploymentMatchCtrl',
    menu: {
      id: 'am.applications.detail.deployment.match',
      state: 'applications.detail.deployment.match',
      key: 'APPLICATIONS.DEPLOYMENT.MATCHING',
      icon: 'fa fa-exchange',
      roles: ['APPLICATION_MANAGER', 'APPLICATION_DEPLOYER'],
      priority: 200,
      step: {
        nextStepId: 'am.applications.detail.deployment.input',
        taskCodes: ['NODE_NOT_SUBSTITUTED', 'IMPLEMENT', 'REPLACE']
      }
    }
  });

  modules.get('a4c-applications').controller('ApplicationDeploymentMatchCtrl',
    ['$scope', 'nodeTemplateService', 'deploymentTopologyServices',
      function($scope, nodeTemplateService, deploymentTopologyServices) {
        $scope.getIcon = function(template) {
          if (!_.isEmpty($scope.deploymentContext.deploymentTopologyDTO.availableSubstitutions.substitutionTypes.nodeTypes)) {
            var templateType = $scope.deploymentContext.deploymentTopologyDTO.availableSubstitutions.substitutionTypes.nodeTypes[template.template.type];
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
            $scope.selectedResourceTemplate = template;
          } else {
            $scope.selectedResourceTemplate = substitutedNode;
          }
        };

        $scope.changeSubstitution = function(nodeName, template) {
          $scope.selectTemplate(nodeName, template);
          var substitutedNode = $scope.getSubstitutedTemplate(nodeName);
          if (substitutedNode.id !== template.id) {
            deploymentTopologyServices.updateSubstitution({
              appId: $scope.application.id,
              envId: $scope.deploymentContext.selectedEnvironment.id,
              nodeId: nodeName,
              locationResourceTemplateId: template.id
            }, undefined, function(response) {
              $scope.updateScopeDeploymentTopologyDTO(response.data);
              $scope.selectedResourceTemplate = $scope.getSubstitutedTemplate(nodeName);
            });
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
          }), function(result) {
            $scope.updateScopeDeploymentTopologyDTO(result.data);
          }).$promise;
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
          }), function(result) {
            $scope.updateScopeDeploymentTopologyDTO(result.data);
          }).$promise;
        };
      }
    ]); //controller
}); //Define
