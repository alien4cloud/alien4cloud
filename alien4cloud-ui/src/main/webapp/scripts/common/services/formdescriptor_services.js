define(function(require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-common', ['ngResource']).factory('formDescriptorServices', ['$resource',
    function($resource) {

      var nodeTypeFormDescriptorDAO = $resource('rest/v1/formdescriptor/nodetype', {}, {
        'get': {
          method: 'GET'
        }
      });

      var tagConfigurationFormDescriptorDAO = $resource('rest/v1/formdescriptor/tagconfiguration', {}, {});

      var pluginConfigFormDescriptorDAO = $resource('rest/v1/formdescriptor/pluginConfig/:pluginId', {}, {
        'get': {
          method: 'GET'
        }
      });

      var getNodeTypeFormDescriptor = function() {
        return nodeTypeFormDescriptorDAO.get().$promise.then(function(result) {
          return result.data;
        });
      };

      var getToscaComplexTypeDescriptor = $resource('rest/v1/formdescriptor/complex-tosca-type');

      return {
        getNodeTypeFormDescriptor: getNodeTypeFormDescriptor,
        getTagConfigurationDescriptor: tagConfigurationFormDescriptorDAO,
        getForPluginConfig: pluginConfigFormDescriptorDAO.get,
        getToscaComplexTypeDescriptor: getToscaComplexTypeDescriptor.save
      };
    }
  ]);
});
