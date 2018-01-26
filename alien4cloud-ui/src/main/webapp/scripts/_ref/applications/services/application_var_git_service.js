define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-applications').factory('appVarGitService', ['$alresource',
    function($alresource) {
      return $alresource('rest/latest/applications/:applicationId/variables/git');
    }
  ]);
});
