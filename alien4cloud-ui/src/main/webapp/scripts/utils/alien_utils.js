// Helper utils for alien4cloud
define(function () {
  'use strict';
  var _ = require('lodash');
  var utils = {};

  utils.fromDeploymentStatusToCssClasses = function(environment) {
    switch (_.get(environment, 'status')) {
      case 'DEPLOYED':
      case 'UPDATED':
        return 'fa-circle text-success';

      case 'UNDEPLOYED':
        return 'fa-circle text-muted';

      case 'WARNING':
      case 'UPDATE_FAILURE':
        return 'fa-warning text-warning';

      case 'FAILURE':
      case 'UNKNOWN':
        return 'fa-question-circle text-muted';

      case 'INIT_DEPLOYMENT':
      case 'DEPLOYMENT_IN_PROGRESS':
      case 'UPDATE_IN_PROGRESS':
      case 'UNDEPLOYMENT_IN_PROGRESS':
          return 'fa-spinner fa-spin';
      default:
       return '';
    }
  };
  return utils;
});
