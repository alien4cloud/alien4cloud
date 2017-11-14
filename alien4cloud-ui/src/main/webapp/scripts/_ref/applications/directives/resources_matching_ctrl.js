define(function (require) {
  'use strict';

  var modules = require('modules');
  var angular = require('angular');
  var _ = require('lodash');

  require('scripts/tosca/services/tosca_service');

  modules.get('a4c-applications').controller('ResourcesMatchingCtrl',
    ['$scope', 'toscaService',
    function ($scope, toscaService) {

      $scope.getIcon = function(template, templateName) {
        console.log(template);
        var templateType;
        if (_.undefined(template)) {
          var templateTypeName = $scope.templates[templateName].type;
          templateType = $scope.templatesTypes[templateTypeName];
        } else if (!_.isEmpty($scope.locationResourcesTypes)) {
          templateType = $scope.locationResourcesTypes[template.template.type];
        }
        if (!_.isEmpty(templateType)) {
          return toscaService.getElementIcon(templateType);
        }
      };

      $scope.getSubstitutedTemplate = function(nodeName) {
        return $scope.substitutedNodes[nodeName];
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
        var substitutedNode = $scope.getSubstitutedTemplate(nodeName);
        if (substitutedNode.id !== template.id) {
          $scope.serviceContext.service.updateSubstitution({
            appId: $scope.application.id,
            envId: $scope.environment.id,
            nodeId: nodeName,
            locationResourceTemplateId: template.id
          }, undefined, function(response) {
            $scope.serviceContext.successCallback(response.data);
            $scope.selectTemplate(nodeName, template);
          });
        }
      };

      $scope.updateSubstitutionProperty = function(propertyName, propertyValue) {
        return $scope.serviceContext.service.updateSubstitutionProperty({
          appId: $scope.application.id,
          envId: $scope.environment.id,
          nodeId: $scope.selectedNodeName
        }, angular.toJson({
          propertyName: propertyName,
          propertyValue: propertyValue
        }),function(result) {
          if (_.undefined(result.error)) {
            $scope.serviceContext.successCallback(result.data);
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
        var originalNode =  $scope.originalNodes[$scope.selectedNodeName] || {};
        var locationTemplate = $scope.substitutedResources[$scope.selectedResourceTemplate.id].template || {};
        var originalProperty = _.result(_.find(originalNode.properties, {'key':propertyName}), 'value');
        var originalLocationTemplateProperty = _.result(_.find(locationTemplate.properties, {'key':propertyName}), 'value');

        if(isPropertyValueNull(originalProperty) && isPropertyValueNull(originalLocationTemplateProperty)){
          return true;
        }
        return false;
      }
      $scope.isNodePropertyEditable = isNodePropertyEditable;

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

      if(_.isFunction($scope.populateScope)){
        $scope.populateScope({scope:$scope});
      }
    }

  ]);
});
