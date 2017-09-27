define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-common').factory('userContextServices', function () {
    return {
      appNavContext: {},
      getAppNavContext: function (applicationId) {
        return this.appNavContext[applicationId];
      },
      setEnvironmentId: function (applicationId, environmentId) {
        this.appNavContext[applicationId] = { type: 'environment', id: environmentId};
      },
      setVersionId: function (applicationId, versionId) {
        this.appNavContext[applicationId] = { type: 'version', id: versionId};
      },
      clear: function(applicationId) {
        if(_.defined(applicationId)) {
          delete this.appNavContext[applicationId];
        } else {
          this.appNavContext = {};
        }
      }
    };
  }); // factory
}); // define
