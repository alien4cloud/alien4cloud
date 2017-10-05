/**
* Directive used to manage the edition of the properties of a policy template.
* Note this directive is not used in the TOSCA topology editor as the editor provides some specific options related to the topology (targets)
*/
define(function(require) {
  'use strict';

  var modules = require('modules');

  require('scripts/tosca/controllers/template_edit_ctrl');

  modules.get('a4c-tosca').directive('a4cPolicyTemplateEdit', function() {
    return {
      templateUrl: 'views/tosca/policy_template_edit.html',
      controller: 'a4cTemplateEditCtrl',
      restrict: 'E',
      scope: {
        'template': '=', // This is the actual policy template to edit.
        'type': '=', // The type of the policy template.
        'resourceDataTypes': '=', // map of data types
        'dependencies': '=', // dependencies
        'onPropertyUpdate': '&', // callback operation triggered when a property is actually updated
        'isPropertyEditable': '&?', // callback operation that should return true if a property or a capability property can be edited.
        // 'isService': '=?', // is it a service ?
      },
      link: {}
    };
  });
});
