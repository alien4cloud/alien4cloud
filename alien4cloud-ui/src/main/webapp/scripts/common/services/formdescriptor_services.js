define(function(require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-common', ['ngResource']).factory('formDescriptorServices', ['$resource',
    function($resource) {

      var nodeTypeFormDescriptorDAO = $resource('rest/latest/formdescriptor/nodetype', {}, {
        'get': {
          method: 'GET'
        }
      });

      var tagConfigurationFormDescriptorDAO = $resource('rest/latest/formdescriptor/tagconfiguration', {}, {});

      var pluginConfigFormDescriptorDAO = $resource('rest/latest/formdescriptor/pluginConfig/:pluginId', {}, {
        'get': {
          method: 'GET'
        }
      });

      var getNodeTypeFormDescriptor = function() {
        return nodeTypeFormDescriptorDAO.get().$promise.then(function(result) {
          return result.data;
        });
      };

      var getToscaComplexTypeDescriptor = $resource('rest/latest/formdescriptor/complex-tosca-type');

      return {
        getNodeTypeFormDescriptor: getNodeTypeFormDescriptor,
        getTagConfigurationDescriptor: tagConfigurationFormDescriptorDAO,
        getForPluginConfig: pluginConfigFormDescriptorDAO.get,
        getToscaComplexTypeDescriptor: getToscaComplexTypeDescriptor.save
      };
    }
  ]);
});
