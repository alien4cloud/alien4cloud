'use strict';

angular.module('alienUiApp').factory('applicationServices', ['$resource',
  function($resource) {

    //
    // APPLICATION DEPLOYMENT API
    //
    var applicationRuntimeDAO = $resource('rest/applications/:applicationId/environments/:applicationEnvironmentId/deployment/informations', {}, {});

    var applicationDeploymentSetupDAO = $resource('rest/applications/:applicationId/environments/:applicationEnvironmentId/deployment-setup', {}, {
      'get': {
        method: 'GET'
      },
      'update': {
        method: 'PUT'
      }
    });

    var applicationActiveDeploymentDAO = $resource('rest/applications/:applicationId/environments/:applicationEnvironmentId/active-deployment');

    var applicationDeploymentDAO = $resource('rest/applications/:applicationId/environments/:applicationEnvironmentId/deployment', {}, {
      'status': {
        method: 'GET'
      },
      'undeploy': {
        method: 'DELETE'
      }
    });

    var applicationDeployment = $resource('rest/applications/deployment', {}, {
      'deploy': {
        method: 'POST'
      }
    });

    var applicationStatus = $resource('rest/applications/statuses', {}, {
      'statuses': {
        method: 'POST'
      }
    });

    var ApplicationScalingDAO = $resource('rest/applications/:applicationId/environments/:applicationEnvironmentId/scale/:nodeTemplateId', {}, {
      'scale': {
        method: 'POST'
      }
    });

    var deploymentProperty = $resource('rest/applications/check-deployment-property', {}, {
      'check': {
        method: 'POST'
      }
    });

    //
    // APPLICATION API
    //
    var applicationCreate = $resource('rest/applications', {}, {
      'create': {
        method: 'POST',
        isArray: false,
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });

    var applicationSearch = $resource('rest/applications/search', {}, {
      'search': {
        method: 'POST',
        isArray: false,
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });

    var applicationDAO = $resource('rest/applications/:applicationId', {}, {
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

    // Application tags & properties
    var applicationProperty = $resource('rest/applications/:applicationId/properties', {}, {
      'upsert': {
        method: 'POST',
        isArray: false,
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });

    var applicationTags = $resource('rest/applications/:applicationId/tags/:tagKey', {}, {
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

    // Cloud resource for the deployment
    var cloudResourcesDAO = $resource('rest/applications/:applicationId/environments/:applicationEnvironmentId/cloud-resources', {}, {});

    // Handle roles on application
    var manageAppUserRoles = $resource('rest/applications/:applicationId/userRoles/:username/:role', {}, {
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

    var manageAppGroupRoles = $resource('rest/applications/:applicationId/groupRoles/:groupId/:role', {}, {
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
      'upsertProperty': applicationProperty.upsert,
      'getActiveDeployment': applicationActiveDeploymentDAO,
      'deployment': applicationDeploymentDAO,
      'deployApplication': applicationDeployment,
      'runtime': applicationRuntimeDAO,
      'scale': ApplicationScalingDAO.scale,
      'tags': applicationTags,
      'userRoles': manageAppUserRoles,
      'groupRoles': manageAppGroupRoles,
      'applicationStatus': applicationStatus,
      'checkProperty': deploymentProperty.check,
      'matchResources': cloudResourcesDAO.get,
      'getDeploymentSetup': applicationDeploymentSetupDAO.get,
      'updateDeploymentSetup': applicationDeploymentSetupDAO.update,
      'create': applicationCreate.create,
      'search': applicationSearch.search
    };
  }
]);
