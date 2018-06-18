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
    getTaskStatusIconCss: function(task) {
      switch (_.get(task, 'status')) {
        case 'SUCCEEDED':
          return 'fa-circle text-success';
        case 'FAILED':
          return 'fa-circle text-danger';
        case 'CANCELLED':
          return 'fa-circle text-warning';
        case 'SCHEDULED':
        case 'STARTED':
          return 'fa-spinner fa-spin text-primary';
        default:
          return '';
      }
    },
    getTaskStatusTextCss: function(task) {
      switch (_.get(task, 'status')) {
        case 'SUCCEEDED':
          return 'text-success';
        case 'FAILED':
          return 'text-danger';
        case 'CANCELLED':
          return 'text-warning';
        case 'SCHEDULED':
        case 'STARTED':
          return 'text-primary';
        default:
          return '';
      }
    },
    getExecutionStatusIconCss: function(execution) {
      switch (_.get(execution, 'status')) {
        case 'SUCCEEDED':
          return (execution.hasFailedTasks) ? 'fa-exclamation-circle text-success' : 'fa-circle text-success';
        case 'FAILED':
          return 'fa-circle text-danger';
        case 'CANCELLED':
          return 'fa-circle text-warning';
        case 'RUNNING':
          return 'fa-spinner fa-spin text-primary';
        default:
          return '';
      }
    },
    getExecutionStatusTextCss: function(execution) {
      switch (_.get(execution, 'status')) {
        case 'SUCCEEDED':
          return 'text-success';
        case 'FAILED':
          return 'text-danger';
        case 'RUNNING':
          return 'text-primary';
        case 'CANCELLED':
          return 'text-warning';
        default:
          return 'text-warning';
      }
    },
    getExecutionStatusCss: function(executionStatus) {
      switch (executionStatus) {
        case 'SUCCEEDED':
          return 'success';
        case 'FAILED':
          return 'danger';
        case 'RUNNING':
          return 'info';
        case 'CANCELLED':
          return 'warning';
        default:
          return 'warning';
      }
    },
    getStatusColor: function(environmentDTO) {
      return colors[_.get(environmentDTO, 'status')];
    }
  };
});
