define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

  states.state('applications.detail.environment.deploynext.matching', {
    url: '/matching',
    templateUrl: 'views/_ref/applications/applications_detail_environment_deploynext_matching.html',
    controller: 'AppEnvDeployNextMatchingCtrl',
    menu: {
      id: 'applications.detail.environment.deploynext.matching',
      state: 'applications.detail.environment.deploynext.matching',
      key: 'NAVAPPLICATIONS.MENU_DEPLOY_NEXT.MATCHING',
      icon: '',
      priority: 500,
      step: {
        taskCodes: ['NO_NODE_MATCHES', 'NODE_NOT_SUBSTITUTED', 'IMPLEMENT', 'REPLACE']
      }
    }
  });

  modules.get('a4c-applications').controller('AppEnvDeployNextMatchingCtrl',
    ['$scope', 'nodeTemplateService', 'deploymentTopologyServices', 'breadcrumbsService', '$translate', '$state',
    function ($scope, nodeTemplateService, deploymentTopologyServices, breadcrumbsService, $translate, $state) {

      breadcrumbsService.putConfig({
        state : 'applications.detail.environment.deploynext.matching',
        text: function(){
          return $translate.instant('NAVAPPLICATIONS.MENU_DEPLOY_NEXT.MATCHING');
        },
        onClick: function(){
          $state.go('applications.detail.environment.deploynext.matching');
        } 
      });

      $scope.getIcon = function(template, templateName) {
        var templateType;
        if (_.undefined(template)) {
          var templateTypeName = $scope.deploymentTopologyDTO.topology.nodeTemplates[templateName].type;
          templateType = $scope.deploymentTopologyDTO.nodeTypes[templateTypeName];
        } else if (!_.isEmpty($scope.deploymentTopologyDTO.availableSubstitutions.substitutionTypes.nodeTypes)) {
          templateType = $scope.deploymentTopologyDTO.availableSubstitutions.substitutionTypes.nodeTypes[template.template.type];
        }
        if (!_.isEmpty(templateType)) {
          return nodeTemplateService.getNodeTypeIcon(templateType);
        }
      };

      $scope.getSubstitutedTemplate = function(nodeName) {
        return $scope.deploymentTopologyDTO.topology.substitutedNodes[nodeName];
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
        return deploymentTopologyServices.updateSubstitutionProperty({
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

      $scope.updateSubstitutionCapabilityProperty = function(capabilityName, propertyName, propertyValue) {
        return deploymentTopologyServices.updateSubstitutionCapabilityProperty({
          appId: $scope.application.id,
          envId: $scope.environment.id,
          nodeId: $scope.selectedNodeName,
          capabilityName: capabilityName
        }, angular.toJson({
          propertyName: propertyName,
          propertyValue: propertyValue
        }), function(result) {
          if (_.undefined(result.error)) {
            $scope.updateScopeDeploymentTopologyDTO(result.data);
          }
          return result;
        }).$promise;
      };

      $scope.isServiceRunning = function(resourceTemplate) {
        return _.defined(resourceTemplate.template.attributeValues) && _.defined(resourceTemplate.template.attributeValues) && _.defined(resourceTemplate.template.attributeValues.state) && resourceTemplate.template.attributeValues.state !== 'initial';
      };

      function isPropertyValueNull(value) {
        return _.undefined(value) || _.undefinedPath(value, 'value');
      }

      function isNodePropertyEditable(propertyName) {
        var originalNode =  $scope.deploymentTopologyDTO.topology.originalNodes[$scope.selectedNodeName] || {};
        var locationTemplate = $scope.deploymentTopologyDTO.locationResourceTemplates[$scope.selectedResourceTemplate.id].template || {};
        var originalProperty = _.result(_.find(originalNode.properties, {'key':propertyName}), 'value');
        var originalLocationTemplateProperty = _.result(_.find(locationTemplate.properties, {'key':propertyName}), 'value');

        if(isPropertyValueNull(originalProperty) && isPropertyValueNull(originalLocationTemplateProperty)){
          return true;
        }
        return false;
      }

      function isCapabilityPropertyEditable(capabilityName, propertyName) {
        var originalNode =  $scope.deploymentTopologyDTO.topology.originalNodes[$scope.selectedNodeName] || {};
        var locationTemplate = $scope.deploymentTopologyDTO.locationResourceTemplates[$scope.selectedResourceTemplate.id].template || {};
        var originalCapability = _.result(_.find(originalNode.capabilities, {'key':capabilityName}), 'value') || {};
        var locationTemplateCapability = _.result(_.find(locationTemplate.capabilities, {'key':capabilityName}), 'value') || {};
        var originalCapaProp = _.result(_.find(originalCapability.properties, {'key':propertyName}), 'value');
        var originalLocationTemplateCapaProp = _.result(_.find(locationTemplateCapability.properties, {'key':propertyName}), 'value');

        if(isPropertyValueNull(originalCapaProp) && isPropertyValueNull(originalLocationTemplateCapaProp)){
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
          if(_.definedPath(propertyPath, 'capabilityName')){
            return isCapabilityPropertyEditable(propertyPath.capabilityName, propertyPath.propertyName);
          }else{
            return isNodePropertyEditable(propertyPath.propertyName);
          }
        }
        return false;
      };
    }
  ]);
});
