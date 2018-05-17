// Helper utils for alien4cloud
define(function () {
  'use strict';
  var _ = require('lodash');

  var colors = {
    'DEPLOYED': '#398439',
    'UPDATED': '#398439',
    'UNDEPLOYED': '#D8D8D8',
    'UNKNOWN': '#505050',
    'WARNING': '#DE9600',
    'FAILURE': '#C51919',
    'DEPLOYMENT_IN_PROGRESS': '#2C80D3',
    'UNDEPLOYMENT_IN_PROGRESS': '#D0ADAD'
  };

  return {
    getStatusIconCss: function(environmentDTO) {
      switch (_.get(environmentDTO, 'status')) {
        case 'DEPLOYED':
        case 'UPDATED':
          return 'fa-circle text-success';

        case 'UNDEPLOYED':
          return 'fa-circle text-muted';

        case 'WARNING':
        case 'UPDATE_FAILURE':
          return 'fa-warning text-warning';

        case 'FAILURE':
          return 'fa-circle text-danger';

        case 'UNKNOWN':
          return 'fa-question-circle text-muted';

        case 'INIT_DEPLOYMENT':
        case 'DEPLOYMENT_IN_PROGRESS':
        case 'UPDATE_IN_PROGRESS':
        case 'UNDEPLOYMENT_IN_PROGRESS':
          return 'fa-spinner fa-spin text-primary';
        default:
          return '';
      }
    },
    getStatusTextCss: function(environmentDTO) {
      switch (_.get(environmentDTO, 'status')) {
        case 'DEPLOYED':
        case 'UPDATED':
          return 'text-success';

        case 'UNDEPLOYED':
          return 'text-muted';

        case 'WARNING':
        case 'UPDATE_FAILURE':
          return 'text-warning';

        case 'FAILURE':
          return 'text-danger';

        case 'UNKNOWN':
          return 'fa-question-circle text-muted';

        case 'INIT_DEPLOYMENT':
        case 'DEPLOYMENT_IN_PROGRESS':
        case 'UPDATE_IN_PROGRESS':
        case 'UNDEPLOYMENT_IN_PROGRESS':
          return 'text-primary';
        default:
          return '';
      }
    },
    getExecutionStatusIconCss: function(execution) {
      switch (_.get(execution, 'status')) {
        case 'SUCCEEDED':
          return 'fa-circle text-success';
        case 'FAILED':
          return 'fa-circle text-danger';
        case 'RUNNING':
          return 'fa-spinner fa-spin text-primary';
        default:
          return '';
      }
    },
    getExecutionStatusTextCss: function(environmentDTO) {
      switch (_.get(environmentDTO, 'status')) {
        case 'SUCCEEDED':
          return 'text-success';
        case 'FAILED':
          return 'text-danger';
        case 'RUNNING':
          return 'text-primary';
        default:
          return '';
      }
    },
    getStatusColor: function(environmentDTO) {
      return colors[_.get(environmentDTO, 'status')];
    }
  };
});
