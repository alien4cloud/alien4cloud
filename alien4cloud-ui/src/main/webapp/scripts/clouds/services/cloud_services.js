// list of cloud images that can be defined for multiple clouds actually.
define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-clouds', ['ngResource']).factory('cloudServices', ['$resource',
    function($resource) {

      var networkFormDescriptor = {
        "_type": "complex",
        "_order": [ "networkName", "ipVersion", "isExternal", "cidr", "gatewayIp", "description"],
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
          "isExternal": {
            "_label": "CLOUDS.NETWORKS.IS_EXTERNAL",
            "_type": "boolean",
            "_notNull": true
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
          },
          "description": {
            "_label": "CLOUDS.DESCRIPTION",
            "_type": "string",
            "_notNull": false
          }
        }
      };

      var storageFormDescriptor = {
          "_type": "complex",
          "_order": [ "id", "device", "size", "description"],
          "_propertyType": {
            "id": {
              "_label": "CLOUDS.STORAGES.ID",
              "_type": "string",
              "_notNull": true
            },
            "device": {
              "_label": "CLOUDS.STORAGES.DEVICE",
              "_type": "string",
              "_notNull": false
            },
            "size": {
              "_label": "CLOUDS.STORAGES.SIZE",
              "_type": "number",
              "_notNull": true,
              "_step": 0.5,
              "_unit": "GiB",
              "_multiplier": 1024 * 1024 * 1024,
              "_constraints": [
                {
                  "greaterThan": 0
                }
              ]
            },
            "description": {
              "_label": "CLOUDS.DESCRIPTION",
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
            "_unit": "GiB",
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
            "_unit": "MiB",
            "_multiplier": 1024 * 1024,
            "_constraints": [
              {
                "greaterOrEqual": 1
              }
            ]
          }
        }
      };

      var zoneFormDescriptor = {
          "_type": "complex",
          "_order": [ "id", "description"],
          "_propertyType": {
            "id": {
              "_label": "CLOUDS.ZONES.ID",
              "_type": "string",
              "_notNull": true
            },
            "description": {
              "_label": "CLOUDS.DESCRIPTION",
              "_type": "string",
              "_notNull": false
            }
          }
        };

      var crudImage = $resource('rest/clouds/:id/images/:imageId');

      var crudFlavor = $resource('rest/clouds/:id/flavors/:flavorId');

      var crudNetwork = $resource('rest/clouds/:id/networks/:networkName');

      var crudStorage = $resource('rest/clouds/:id/storages/:storageId');

      var crudZone = $resource('rest/clouds/:id/zones/:zoneId');

      var setCloudTemplateStatus = $resource('rest/clouds/:id/templates/:activableComputeId/status');

      var setCloudTemplateResource = $resource('rest/clouds/:id/templates/:imageId/:flavorId/resource');

      var cloudImageResource = $resource('rest/clouds/:id/images/:resourceId/resource');

      var cloudFlavorResource = $resource('rest/clouds/:id/flavors/:resourceId/resource');

      var cloudNetworkResource = $resource('rest/clouds/:id/networks/:resourceId/resource');

      var cloudStorageResource = $resource('rest/clouds/:id/storages/:resourceId/resource');

      var cloudZoneResource = $resource('rest/clouds/:id/zones/:resourceId/resource');

      var cloudRefreshResource = $resource('rest/clouds/:id/refresh');

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

      var cloudProperty = $resource('rest/clouds/:cloudId/properties', {}, {
        'upsert': {
          method: 'POST',
          isArray: false,
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          }
        }
      });

      var cloneCloud = $resource('rest/clouds/:id/clone');

      return {
        'create': crudCloud.create,
        'get': crudCloud.get,
        'refresh': cloudRefreshResource.get,
        'update': crudCloud.update,
        'remove': crudCloud.remove,
        'config': CloudConfiguration,
        'userRoles': manageCloudUserRoles,
        'groupRoles': manageCloudGroupRoles,
        'upsertProperty': cloudProperty.upsert,
        'flavorFormDescriptor': flavorFormDescriptor,
        'addFlavor': crudFlavor.save,
        'removeFlavor': crudFlavor.remove,
        'addImage': crudImage.save,
        'removeImage': crudImage.remove,
        'networkFormDescriptor': networkFormDescriptor,
        'addNetwork': crudNetwork.save,
        'removeNetwork': crudNetwork.remove,
        'storageFormDescriptor': storageFormDescriptor,
        'zoneFormDescriptor': zoneFormDescriptor,
        'addStorage': crudStorage.save,
        'removeStorage': crudStorage.remove,
        'addZone': crudZone.save,
        'removeZone': crudZone.remove,
        'setCloudTemplateStatus': setCloudTemplateStatus.save,
        'setCloudTemplateResource': setCloudTemplateResource.save,
        'setCloudNetworkResource': cloudNetworkResource.save,
        'setCloudImageResource' : cloudImageResource.save,
        'setCloudFlavorResource' : cloudFlavorResource.save,
        'setCloudStorageResource' : cloudStorageResource.save,
        'setCloudZoneResource' : cloudZoneResource.save,
        'cloneCloud' : cloneCloud.save
      };
    }
  ]);
});
