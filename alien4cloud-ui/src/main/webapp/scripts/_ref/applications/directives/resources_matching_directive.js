define(function(require) {
  'use strict';

  var modules = require('modules');

  require('scripts/_ref/applications/directives/resources_matching_ctrl');

  modules.get('a4c-applications').directive('resourcesMatching', function() {
    return {
      templateUrl: 'views/_ref/applications/directives/resources_matching.html',
      restrict: 'E',
      controller: 'ResourcesMatchingCtrl',
      scope: {
        /**
        **{
        availableSubstitutions: ndoeId -> List(locationResourceTemplateId)
        substitutedResources: nodeId -> locationResourceTemplate
        substitutedNodes: nodeId->locationResourceTemplateId
        templates: nodeId -> template
        originalNodes: nodeId -> template
        substitutionTypes:
        templatesTypes:
        topology:
        serviceContext: {service, successCalback}
        populateScope: function(scope)
      }
        **/
        'context': '=?',
        'availableSubstitutions': '=',
        'substitutedResources':'=',
        'substitutedNodes':'=',
        'templates':'=',
        'originalNodes':'=',
        'substitutionTypes': '=allSubstitutionTypes',
        'templatesTypes': '=',
        'locationResourcesTypes': '=',
        'topology': '=',
        'serviceContext': '=',
        'application':'=',
        'environment':'=',
        'populateScope':'&',
        'resourceTemplateEditDisplayUrl':'@templateEditUrl'
      },
      link: {}
    };
  });
});
