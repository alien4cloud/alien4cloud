define(function(require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-orchestrators', ['a4c-common']).factory('locationService', ['$alresource',
    function($alresource) {
      return $alresource('rest/v1/orchestrators/:orchestratorId/locations/:locationId');
    }
  ]);

  modules.get('a4c-orchestrators', ['a4c-common']).factory('locationResourcesService', ['$alresource',
    function($alresource) {
      return $alresource('rest/v1/orchestrators/:orchestratorId/locations/:locationId/resources/:id');
    }
  ]);

  modules.get('a4c-orchestrators', ['a4c-common']).factory('locationResourcesPropertyService', ['$alresource',
    function($alresource) {
      return $alresource('rest/v1/orchestrators/:orchestratorId/locations/:locationId/resources/:id/template/properties');
    }
  ]);

  modules.get('a4c-orchestrators', ['a4c-common']).factory('locationResourcesCapabilityPropertyService', ['$alresource',
    function($alresource) {
      return $alresource('rest/v1/orchestrators/:orchestratorId/locations/:locationId/resources/:id/template/capabilities/:capabilityName/properties');
    }
  ]);
});
