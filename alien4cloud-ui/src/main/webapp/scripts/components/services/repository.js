define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-components', ['a4c-common']).factory('repositoryService', ['$alresource',
    function ($alresource) {
      return $alresource('rest/latest/repositories/:repositoryId');
    }
  ]);

  modules.get('a4c-components', ['a4c-common']).factory('repositoryPluginService', ['$alresource',
    function ($alresource) {
      return $alresource('rest/latest/repository-plugins');
    }
  ]);

  modules.get('a4c-components', ['a4c-common']).factory('repositoryPluginConfigurationService', ['$alresource',
    function ($alresource) {
      return $alresource('rest/latest/formdescriptor/repositoryConfig/:pluginId');
    }
  ]);
});
