define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-deployment').directive('displayOutputs', function() {
    return {
      restrict: 'E',
      templateUrl: 'views/deployment/display_outputs.html',
      // inherites scope from the parent
      scope: true,
      link: function(scope, element, attrs) {
        scope._ = _;
        scope.collapsable = scope.$eval(attrs.collapsable);
        scope.classes = scope.$eval(attrs.classes);

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
