'use strict';

angular.module('alienUiApp').factory('cloudServices', ['$resource',
  function($resource) {

    var networkFormDescriptor = {
      "_type": "complex",
      "_order": [ "networkName", "ipVersion", "cidr", "gatewayIp"],
      "_propertyType": {
        "networkName": {
          "_label": "CLOUDS.NETWORKS.NAME",
          "_type": "string",
          "_notNull": true
        },
        "ipVersion": {
          "_label": "CLOUDS.NETWORKS.IP_VERSION",
          "_type": "number",
          "_notNull": true,
          "_validValues": [4, 6]
        },
        "cidr": {
          "_label": "CLOUDS.NETWORKS.CIDR",
          "_type": "string",
          "_notNull": false
        },
        "gatewayIp": {
          "_label": "CLOUDS.NETWORKS.GATEWAY_IP",
          "_type": "string",
          "_notNull": false
        }
      }
    };

    var flavorFormDescriptor = {
      "_type": "complex",
      "_order": [ "id", "numCPUs", "diskSize", "memSize"],
      "_propertyType": {
        "id": {
          "_label": "CLOUDS.FLAVORS.ID",
          "_type": "string",
          "_notNull": true
        },
        "numCPUs": {
          "_label": "CLOUDS.FLAVORS.NUM_CPUS",
          "_type": "number",
          "_notNull": true,
          "_step": 1,
          "_constraints": [
            {
              "greaterOrEqual": 1
            }
          ]
        },
        "diskSize": {
          "_label": "CLOUDS.FLAVORS.DISK_SIZE",
          "_type": "number",
          "_notNull": true,
          "_step": 0.01,
          "_unit": "GB",
          "_multiplier": 1024 * 1024 * 1024,
          "_constraints": [
            {
              "greaterThan": 0
            }
          ]
        },
        "memSize": {
          "_label": "CLOUDS.FLAVORS.MEM_SIZE",
          "_type": "number",
          "_notNull": true,
          "_step": 1,
          "_unit": "MB",
          "_multiplier": 1024 * 1024,
          "_constraints": [
            {
              "greaterOrEqual": 1
            }
          ]
        }
      }
    };

    var crudImage = $resource('rest/clouds/:id/images/:imageId');

    var crudFlavor = $resource('rest/clouds/:id/flavors/:flavorId');

    var crudNetwork = $resource('rest/clouds/:id/networks/:networkName');

    var setCloudTemplateStatus = $resource('rest/clouds/:id/templates/:imageId/:flavorId/status');

    var setCloudTemplateResource = $resource('rest/clouds/:id/templates/:imageId/:flavorId/resource');

    var setNetworkResource = $resource('rest/clouds/:id/networks/:networkName/resource');

    var crudCloud = $resource('rest/clouds/:id', {}, {
      'create': {
        method: 'POST',
        isArray: false,
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      },
      'get': {
        method: 'GET',
        isArray: false,
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      },
      'update': {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      },
      'remove': {
        method: 'DELETE',
        isArray: false,
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });

    var CloudConfiguration = $resource('rest/clouds/:id/configuration', {}, {
      'update': {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });


    /* Users/groups roles on an clouds */
    var manageCloudUserRoles = $resource('rest/clouds/:cloudId/userRoles/:username/:role', {}, {
      'addUserRole': {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        },
        params: {
          cloudId: '@cloudId',
          username: '@username',
          role: '@role'
        }
      },
      'removeUserRole': {
        method: 'DELETE',
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        },
        params: {
          cloudId: '@cloudId',
          username: '@username',
          role: '@role'
        }
      }
    });

    var manageCloudGroupRoles = $resource('rest/clouds/:cloudId/groupRoles/:groupId/:role', {}, {
      'addGroupRole': {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        },
        params: {
          cloudId: '@cloudId',
          groupId: '@groupId',
          role: '@role'
        }
      },
      'removeGroupRole': {
        method: 'DELETE',
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        },
        params: {
          cloudId: '@cloudId',
          groupId: '@groupId',
          role: '@role'
        }
      }
    });

    return {
      'create': crudCloud.create,
      'get': crudCloud.get,
      'update': crudCloud.update,
      'remove': crudCloud.remove,
      'config': CloudConfiguration,
      'userRoles': manageCloudUserRoles,
      'groupRoles': manageCloudGroupRoles,
      'flavorFormDescriptor': flavorFormDescriptor,
      'addFlavor': crudFlavor.save,
      'removeFlavor': crudFlavor.remove,
      'addImage': crudImage.save,
      'removeImage': crudImage.remove,
      'networkFormDescriptor': networkFormDescriptor,
      'addNetwork': crudNetwork.save,
      'removeNetwork': crudNetwork.remove,
      'setCloudTemplateStatus': setCloudTemplateStatus.save,
      'setCloudTemplateResource': setCloudTemplateResource.save,
      'setNetworkResource': setNetworkResource.save
    };
  }
]);
