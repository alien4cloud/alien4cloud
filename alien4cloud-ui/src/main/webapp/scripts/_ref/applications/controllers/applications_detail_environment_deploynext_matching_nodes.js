define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

  require('scripts/_ref/applications/directives/resources_matching_directive');

  states.state('applications.detail.environment.deploynext.matching.nodes', {
    url: '/nodes',
    templateUrl: 'views/_ref/applications/applications_detail_environment_deploynext_matching_nodes.html',
    controller: 'AppEnvDeployNextNodesMatchingCtrl',
    menu: {
      id: 'applications.detail.environment.deploynext.matching.nodes',
      state: 'applications.detail.environment.deploynext.matching.nodes',
      key: 'APPLICATIONS.DEPLOYMENT.MATCHING.NODES_TITLE',
      icon: '',
      priority: 200,
      step: {
        taskCodes: ['NO_NODE_MATCHES', 'NODE_NOT_SUBSTITUTED', 'IMPLEMENT', 'REPLACE']
      }
    }
  });

  modules.get('a4c-applications').controller('AppEnvDeployNextNodesMatchingCtrl',
    ['$scope', 'nodeTemplateService', 'deploymentTopologyServices', 'topoEditProperties',
    function ($scope, nodeTemplateService, deploymentTopologyServices, topoEditProperties) {

      topoEditProperties($scope);

      function isPropertyValueNull(value) {
        return _.undefined(value) || _.undefinedPath(value, 'value');
      }

      function isCapabilityPropertyEditable(capabilityName, propertyName, selectedResourceTemplate) {
        var originalNode =  $scope.deploymentTopologyDTO.topology.originalNodes[$scope.selectedNodeName] || {};
        var locationTemplate = $scope.deploymentTopologyDTO.locationResourceTemplates[selectedResourceTemplate.id].template || {};
        var originalCapability = _.result(_.find(originalNode.capabilities, {'key':capabilityName}), 'value') || {};
        var locationTemplateCapability = _.result(_.find(locationTemplate.capabilities, {'key':capabilityName}), 'value') || {};
        var originalCapaProp = _.result(_.find(originalCapability.properties, {'key':propertyName}), 'value');
        var originalLocationTemplateCapaProp = _.result(_.find(locationTemplateCapability.properties, {'key':propertyName}), 'value');

        if(isPropertyValueNull(originalCapaProp) && isPropertyValueNull(originalLocationTemplateCapaProp)){
          return true;
        }
        return false;
      }

      function isSecretCapabilityPropertyEditable(capabilityName, propertyName, selectedResourceTemplate) {
        var originalNode =  $scope.deploymentTopologyDTO.topology.originalNodes[$scope.selectedNodeName] || {};
        var locationTemplate = $scope.deploymentTopologyDTO.locationResourceTemplates[selectedResourceTemplate.id].template || {};
        var originalCapability = _.result(_.find(originalNode.capabilities, {'key':capabilityName}), 'value') || {};
        var locationTemplateCapability = _.result(_.find(locationTemplate.capabilities, {'key':capabilityName}), 'value') || {};
        var originalCapaProp = _.result(_.find(originalCapability.properties, {'key':propertyName}), 'value');
        var originalLocationTemplateCapaProp = _.result(_.find(locationTemplateCapability.properties, {'key':propertyName}), 'value');

        if(!$scope.properties.isSecretValue(originalCapaProp) && !$scope.properties.isSecretValue(originalLocationTemplateCapaProp)){
          return true;
        }
        return false;
      }

      $scope.serviceContext = {
        service: deploymentTopologyServices,
        successCallback: $scope.updateScopeDeploymentTopologyDTO
      };

      $scope.populate = function(scope){

        scope.updateSubstitutionCapabilityProperty = function(capabilityName, propertyName, propertyValue) {
          return deploymentTopologyServices.updateSubstitutionCapabilityProperty({
            appId: $scope.application.id,
            envId: $scope.environment.id,
            nodeId: scope.selectedNodeName,
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

        //property is editable only when not set in topology or in locationResource
        //override the directive default isPropertyEditable since we also proceed capability properties
        scope.isPropertyEditable = function(propertyPath) {
          if(scope.selectedResourceTemplate.service) {
            // do not edit servie properties
            return false;
          }
          if(scope.getSubstitutedTemplate(scope.selectedNodeName).id === scope.selectedResourceTemplate.id){
            if(_.definedPath(propertyPath, 'capabilityName')){
              return isCapabilityPropertyEditable(propertyPath.capabilityName, propertyPath.propertyName, scope.selectedResourceTemplate);
            }else{
              return scope.isNodePropertyEditable(propertyPath.propertyName);
            }
          }
          return false;
        };

        //secret is editable only when not set in topology or in locationResource
        //override the directive default isPropertyEditable since we also proceed capability properties
        scope.isSecretEditable = function(propertyPath) {
          if(scope.selectedResourceTemplate.service) {
            // do not edit servie properties
            return true;
          }
          if(scope.getSubstitutedTemplate(scope.selectedNodeName).id === scope.selectedResourceTemplate.id){
            if(_.definedPath(propertyPath, 'capabilityName')){
              return isSecretCapabilityPropertyEditable(propertyPath.capabilityName, propertyPath.propertyName, scope.selectedResourceTemplate);
            }else{
              return scope.isSecretPropertyEditable(propertyPath.propertyName);
            }
          }
          return false;
        };
      };

    }
  ]);
});
