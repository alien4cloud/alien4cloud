define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-applications', ['ngResource']).factory('applicationServices', ['$resource',
    function($resource) {
      // Application deployment api
      var applicationRuntimeDAO = $resource('rest/latest/applications/:applicationId/environments/:applicationEnvironmentId/deployment/informations', {}, {});

      var applicationDeploymentSetupDAO = $resource('rest/latest/applications/:applicationId/environments/:applicationEnvironmentId/deployment-setup', {}, {
        'get': {
          method: 'GET'
        },
        'update': {
          method: 'PUT'
        }
      });

      var applicationActiveDeploymentDAO = $resource('rest/latest/applications/:applicationId/environments/:applicationEnvironmentId/active-deployment');
      var applicationMonitoredDeploymentDAO = $resource('rest/latest/applications/:applicationId/environments/:applicationEnvironmentId/active-deployment-monitored');

      var applicationSecretProviderConfigurationDAO = $resource('rest/latest/applications/:applicationId/environments/:applicationEnvironmentId/current-secret-provider-configurations');

      var applicationRuntimeTopologyDAO = $resource('rest/latest/applications/:applicationId/environments/:applicationEnvironmentId/runtime-topology');

      var applicationDeploymentDAO = $resource('rest/latest/applications/:applicationId/environments/:applicationEnvironmentId/deployment', {}, {
        'undeploy': {
          method: 'PUT'
        }
      });

      var applicationDeployment = $resource('rest/latest/applications/deployment', {}, {
        'deploy': {
          method: 'POST'
        }
      });

      var applicationDeploymentUpdate = $resource('rest/latest/applications/:applicationId/environments/:applicationEnvironmentId/update-deployment', {}, {
        'update': {
          method: 'POST'
        }
      });

      var envrironments = $resource('rest/latest/applications/environments', {}, {
        'getAll': {
          method: 'POST'
        }
      });

      var ApplicationScalingDAO = $resource('rest/latest/applications/:applicationId/environments/:applicationEnvironmentId/scale/:nodeTemplateId', {}, {
        'scale': {
          method: 'POST'
        }
      });

      var applicationWorkflowResource = $resource('rest/latest/applications/:applicationId/environments/:applicationEnvironmentId/workflows/:workflowName', {}, {
        'launch': {
          method: 'POST'
        }
      });

      var deploymentProperty = $resource('rest/latest/orchestrators/:orchestratorId/deployment-prop-check', {}, {
        'check': {
          method: 'POST'
        }
      });

      //
      // APPLICATION API
      //
      var applicationCreate = $resource('rest/latest/applications', {}, {
        'create': {
          method: 'POST',
          isArray: false,
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          }
        }
      });

      var applicationSearch = $resource('rest/latest/applications/search', {}, {
        'search': {
          method: 'POST',
          isArray: false,
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          }
        }
      });

      var applicationDAO = $resource('rest/latest/applications/:applicationId', {}, {
        'get': {
          method: 'GET'
        },
        'remove': {
          method: 'DELETE'
        },
        'update': {
          method: 'PUT'
        }
      });

      var applicationTags = $resource('rest/latest/applications/:applicationId/tags/:tagKey', {}, {
        'upsert': {
          method: 'POST',
          isArray: false,
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          }
        },
        'remove': {
          method: 'DELETE',
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          }
        }
      });

      // Handle roles on application
      var manageAppUserRoles = $resource('rest/latest/applications/:applicationId/roles/users/:username/:role', {}, {
        'addUserRole': {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          },
          params: {
            applicationId: '@applicationId',
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
            applicationId: '@applicationId',
            username: '@username',
            role: '@role'
          }
        }
      });

      var manageAppGroupRoles = $resource('rest/latest/applications/:applicationId/roles/groups/:groupId/:role', {}, {
        'addGroupRole': {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          },
          params: {
            applicationId: '@applicationId',
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
            applicationId: '@applicationId',
            groupId: '@groupId',
            role: '@role'
          }
        }
      });

      return {
        'get': applicationDAO.get,
        'remove': applicationDAO.remove,
        'update': applicationDAO.update,
        'getActiveDeployment': applicationActiveDeploymentDAO,
        'getActiveMonitoredDeployment': applicationMonitoredDeploymentDAO,
        'getSecretProviderConfigurationsForCurrentDeployment': applicationSecretProviderConfigurationDAO,
        'getRuntimeTopology': applicationRuntimeTopologyDAO,
        'deployment': applicationDeploymentDAO,
        'deploymentUpdate': applicationDeploymentUpdate.update,
        'deployApplication': applicationDeployment,
        'runtime': applicationRuntimeDAO,
        'scale': ApplicationScalingDAO.scale,
        'tags': applicationTags,
        'userRoles': manageAppUserRoles,
        'groupRoles': manageAppGroupRoles,
        'envrironments': envrironments,
        'checkProperty': deploymentProperty.check,
        'getDeploymentSetup': applicationDeploymentSetupDAO.get,
        'updateDeploymentSetup': applicationDeploymentSetupDAO.update,
        'create': applicationCreate.create,
        'search': applicationSearch.search,
        'launchWorkflow' : applicationWorkflowResource.launch
      };
    }
  ]);
});
