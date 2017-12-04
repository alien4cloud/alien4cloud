/**
* Directive used to manage the edition of the properties of a node template.
* Note this directive is not used in the TOSCA topology editor as the editor provides some specific options related to the topology (input/outputs/)
*/
define(function(require) {
  'use strict';

  var modules = require('modules');

  require('scripts/tosca/controllers/node_template_edit_ctrl');

  modules.get('a4c-tosca').directive('a4cNodeTemplateEdit', function() {
    return {
      templateUrl: 'views/tosca/node_template_edit.html',
      controller: 'a4cNodeTemplateEditCtrl',
      restrict: 'E',
      scope: {
        'template': '=', // This is the actual node template to edit.
        'type': '=', // The type of the node template.
        'nodeCapabilityTypes': '=', // map of capability types
        'resourceDataTypes': '=', // map of capability types
        'dependencies': '=', // dependencies
        'onPropertyUpdate': '&', // callback operation triggered when a property is actually updated
        'isPropertyEditable': '&?', // callback operation that should return true if a property or a capability property can be edited.
        'isSecretEditable': '&?', // callback operation that should return true if a secret
        'onCapabilityPropertyUpdate': '&', // callback operation triggered when a capability property is actually updated
        'onHalfRelationshipTypeUpdate' : '&?', // callback operation triggered when an half relationship type is updated (isService should be true)
        'capabilitiesRelationshipTypes' : '=?', // list of relationship type by capabilities (isService should be true)
        'isService': '=?', // is it a service ?
        'isRelEditable': '&?' // callback operation that should return true if a relationship can be edited.
      },
      link: {}
    };
  });
});
