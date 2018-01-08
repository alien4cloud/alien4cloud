define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-applications').factory('environmentsGitService', ['$alresource',
    function($alresource) {
      return $alresource('rest/latest/applications/:applicationId/environments/:environmentId/git');
    }
  ]);
});
