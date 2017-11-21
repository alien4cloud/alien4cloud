define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-deployment').directive('displayOutputs', function() {

    // link output properties based on values that exists in the topology's node templates.
    function doRefreshOutputProperties(scope) {
      scope.outputPropertiesValue = {};
      scope.outputCapabilityPropertiesValue = {};
      var i;
      for (var nodeId in scope.outputProperties) {
        if (scope.outputProperties.hasOwnProperty(nodeId)) {
          scope.outputPropertiesValue[nodeId] = {};
          for (i = 0; i < scope.outputProperties[nodeId].length; i++) {
            var outputPropertyName = scope.outputProperties[nodeId][i];
            scope.outputPropertiesValue[nodeId][outputPropertyName] = scope.nodeTemplates[nodeId].propertiesMap[outputPropertyName].value;
          }
        }
      }

      if (!_.undefined(scope.outputCapabilityProperties)) {
        for (nodeId in scope.outputCapabilityProperties) {
          if (scope.outputCapabilityProperties.hasOwnProperty(nodeId)) {
            scope.outputCapabilityPropertiesValue[nodeId] = {};
            for (var capabilityId in scope.outputCapabilityProperties[nodeId]) {
              if (scope.outputCapabilityProperties[nodeId].hasOwnProperty(capabilityId)) {
                scope.outputCapabilityPropertiesValue[nodeId][capabilityId] = {};
                for (i = 0; i < scope.outputCapabilityProperties[nodeId][capabilityId].length; i++) {
                  var outputCapabilityPropertyName = scope.outputCapabilityProperties[nodeId][capabilityId][i];
                  scope.outputCapabilityPropertiesValue[nodeId][capabilityId][outputCapabilityPropertyName] = scope.nodeTemplates[nodeId].capabilitiesMap[capabilityId].value.propertiesMap[outputCapabilityPropertyName].value;
                }
              }
            }
          }
        }
      }
    }

    function refreshOutputAttributes(scope){
      scope.outputAttributesValue = {};
      if(_.undefined(scope.outputAttributes)) {
        scope.outputAttributes = {};
      }
      _.forEach(scope.topology.instances, function(instancesInfo, nodeId){
        scope.outputAttributesValue[nodeId] = {};
        _.forEach(instancesInfo, function(instanceInfo, instanceId){
          scope.outputAttributesValue[nodeId][instanceId] = {};
          _.forEach(scope.outputAttributes[nodeId], function(outputAttr){
            scope.outputAttributesValue[nodeId][instanceId][outputAttr] = _.get(instanceInfo.attributes, outputAttr);
          });
          if (_.isEmpty(scope.outputAttributesValue[nodeId][instanceId])) {
            delete scope.outputAttributesValue[nodeId][instanceId];
          }
        });
        if (_.isEmpty(scope.outputAttributesValue[nodeId])) {
          delete scope.outputAttributesValue[nodeId];
        }
      });
    }

    function processOutputsDef(scope){
      if(_.definedPath(scope.topology, 'topology')){
        scope.outputProperties = scope.topology.topology.outputProperties;
        scope.outputCapabilityProperties = scope.topology.topology.outputCapabilityProperties;
        scope.outputAttributes = scope.topology.topology.outputAttributes;
        scope.nodeTemplates = scope.topology.topology.nodeTemplates;

        scope.outputPropertiesSize = _.size(scope.outputProperties);
        scope.outputAttributesSize = _.size(scope.outputAttributes);

        scope.outputNodes = _.union(
          _.keys(scope.outputProperties),
          _.keys(scope.outputCapabilityProperties),
          _.keys(scope.outputAttributes)
        );
      }
    }

    return {
      restrict: 'E',
      templateUrl: 'views/deployment/display_outputs.html',
      // inherites scope from the parent
      scope: true,
      link: function(scope, element, attrs) {
        scope.collapsable = scope.$eval(attrs.collapsable);
        scope.classes = scope.$eval(attrs.classes);

        scope.$watch('topology', function(newValue, oldValue){
          if((_.defined(newValue) && _.undefined(scope.outputNodes)) || !_.isEqual(newValue, oldValue)) {
            processOutputsDef(scope);
            doRefreshOutputProperties(scope);
          }
        });

        scope.$watch('topology.instances', function(newValue, oldValue){
          if((_.defined(newValue) && _.undefined(scope.outputAttributesValue)) || !_.isEqual(newValue, oldValue)) {
            refreshOutputAttributes(scope);
          }
        });

        scope.isEmptyOutpts = function(){
          return _.isEmpty(scope.outputAttributesValue) &&
                 _.isEmpty(scope.outputPropertiesValue) &&
                 _.isEmpty(scope.outputCapabilityPropertiesValue);
        };

        scope.somethingToDisplay = function(nodeId) {
          var nodeIds = _.union(_.keys(scope.outputAttributesValue), _.keys(scope.outputPropertiesValue), _.keys(scope.outputCapabilityPropertiesValue));
          return _.include(nodeIds, nodeId);
        };

      }
    };
  });
}); // define
