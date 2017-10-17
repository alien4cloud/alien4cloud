define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

  require('scripts/applications/services/policies_matching_services.js');

  states.state('applications.detail.environment.deploynext.matching.policies', {
    url: '/policies',
    templateUrl: 'views/_ref/applications/applications_detail_environment_deploynext_matching_policies.html',
    controller: 'AppEnvDeployNextPoliciesMatchingCtrl',
    menu: {
      id: 'applications.detail.environment.deploynext.matching.policies',
      state: 'applications.detail.environment.deploynext.matching.policies',
      key: 'APPLICATIONS.DEPLOYMENT.MATCHING.POLICIES_TITLE',
      icon: '',
      priority: 100,
      step: {
        taskCodes: ['NO_NODE_MATCHES', 'NODE_NOT_SUBSTITUTED', 'IMPLEMENT', 'REPLACE']
      }
    }
  });

  modules.get('a4c-applications').controller('AppEnvDeployNextPoliciesMatchingCtrl',
    ['$scope', 'nodeTemplateService', 'policiesMatchingService',
    function ($scope, nodeTemplateService, policiesMatchingService) {

      $scope.getIcon = function(template, templateName) {
        var templateType;
        if (_.undefined(template)) {
          var templateTypeName = $scope.deploymentTopologyDTO.topology.policies[templateName].type;
          templateType = $scope.deploymentTopologyDTO.policyTypes[templateTypeName];
        } else if (!_.isEmpty($scope.deploymentTopologyDTO.availableSubstitutions.substitutionTypes.policyTypes)) {
          templateType = $scope.deploymentTopologyDTO.availableSubstitutions.substitutionTypes.policyTypes[template.template.type];
        }
        if (!_.isEmpty(templateType)) {
          return nodeTemplateService.getNodeTypeIcon(templateType);
        }
      };

      $scope.getSubstitutedTemplate = function(nodeName) {
        return $scope.deploymentTopologyDTO.topology.substitutedPolicies[nodeName];
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
          policiesMatchingService.updateSubstitution({
            appId: $scope.application.id,
            envId: $scope.environment.id,
            nodeId: nodeName,
            locationResourceTemplateId: template.id
          }, undefined, function(response) {
            $scope.updateScopeDeploymentTopologyDTO(response.data);
            $scope.selectedResourceTemplate = $scope.getSubstitutedTemplate(nodeName);
          });
        }
      };

      $scope.updateSubstitutionProperty = function(propertyName, propertyValue) {
        return policiesMatchingService.updateSubstitutionProperty({
          appId: $scope.application.id,
          envId: $scope.environment.id,
          nodeId: $scope.selectedNodeName
        }, angular.toJson({
          propertyName: propertyName,
          propertyValue: propertyValue
        }),function(result) {
          if (_.undefined(result.error)) {
            $scope.updateScopeDeploymentTopologyDTO(result.data);
          }
          return result;
        }).$promise;
      };

      function isPropertyValueNull(value) {
        return _.undefined(value) || _.undefinedPath(value, 'value');
      }

      function isNodePropertyEditable(propertyName) {
        var originalNode =  $scope.deploymentTopologyDTO.topology.originalPolicies[$scope.selectedNodeName] || {};
        var locationTemplate = $scope.deploymentTopologyDTO.policyLocationResourceTemplates[$scope.selectedResourceTemplate.id].template || {};
        var originalProperty = _.result(_.find(originalNode.properties, {'key':propertyName}), 'value');
        var originalLocationTemplateProperty = _.result(_.find(locationTemplate.properties, {'key':propertyName}), 'value');

        if(isPropertyValueNull(originalProperty) && isPropertyValueNull(originalLocationTemplateProperty)){
          return true;
        }
        return false;
      }

      //property is editable only when not set in topology or in locationResource
      $scope.isPropertyEditable = function(propertyPath) {
        if($scope.selectedResourceTemplate.service) {
          // do not edit servie properties
          return false;
        }
        if($scope.getSubstitutedTemplate($scope.selectedNodeName).id === $scope.selectedResourceTemplate.id){
          return isNodePropertyEditable(propertyPath.propertyName);
        }
        return false;
      };
    }
  ]);
});
