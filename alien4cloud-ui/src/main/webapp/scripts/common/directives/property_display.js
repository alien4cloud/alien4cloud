define(function (require) {
  'use strict';

  var modules = require('modules');

  require('angular-xeditable');
  require('scripts/common/controllers/property_display');
  require('scripts/common/filters/strings');

  modules.get('a4c-common').directive('propertyDisplay', function() {
    return {
      templateUrl: 'views/common/property_display.html',
      restrict: 'E',
      scope: {
        'translated': '=',
        'definition': '=',
        'propertyType': '=?',
        'propertyName': '=',
        'propertyValue': '=',
        'relationshipName': '=?',
        'capabilityName': '=?',
        'dependencies': '=?',
        'onSave': '&',
        'onDelete': '&',
        'editable': '=',
        'condensed': '=',
        'deletable': '=?',
        // The context defines what we are editing (a node, a capability, a relationship, an input)
        // so it handle stuffs like nodeId, capabilityId, relationshipId ... depending of the edition context
        // used in suggestions
        // refer to alien4cloud.model.suggestion.SuggestionContextData
        'propEditionContext': '=?',
        // in some context we to call a function to get the context recursively)
        'propEditionContextFn': '&',
        // in some context we to call a function to get the property name for lists or maps)
        'propertyNameFn': '&',
      },
      link: {}
    };
  });
}); // define
