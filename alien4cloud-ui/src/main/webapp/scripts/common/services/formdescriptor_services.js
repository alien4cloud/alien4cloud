'use strict';

angular.module('alienUiApp').factory('formDescriptorServices', ['$resource',
  function($resource) {

    var nodeTypeFormDescriptorDAO = $resource('rest/formdescriptor/nodetype', {}, {
      'get': {
        method: 'GET'
      }
    });

    var tagConfigurationFormDescriptorDAO = $resource('rest/formdescriptor/tagconfiguration', {}, {});

    var pluginConfigFormDescriptorDAO = $resource('rest/formdescriptor/pluginConfig/:pluginId', {}, {
      'get': {
        method: 'GET'
      }
    });

    var getNodeTypeFormDescriptor = function() {
      return nodeTypeFormDescriptorDAO.get().$promise.then(function(result) {
        return result.data;
      });
    };

    return {
      getNodeTypeFormDescriptor: getNodeTypeFormDescriptor,
      getTagConfigurationDescriptor: tagConfigurationFormDescriptorDAO,
      getForPluginConfig: pluginConfigFormDescriptorDAO.get
    };
  }
]);