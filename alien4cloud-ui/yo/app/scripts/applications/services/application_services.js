'use strict';

angular.module('alienUiApp').factory('applicationServices', ['$resource',
  function($resource) {

    /* application details */
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

    var applicationActiveDeploymentDAO = $resource('rest/applications/:applicationId/active-deployment');

    // API REST Definition
    var applicationProperty = $resource('rest/applications/:applicationId/properties', {}, {
      'upsert': {
        method: 'POST',
        isArray: false,
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });

    /* application deployment */
    // Service that gives access to application deployment service
    var applicationDeploymentDAO = $resource('rest/applications/:applicationId/deployment', {}, {
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

    /*application runtime*/
    // Service that gives access to application deployment service
    var applicationRuntimeDAO = $resource('rest/applications/:applicationId/deployment/informations', {}, {});
    var ApplicationScalingDAO = $resource('rest/applications/:applicationId/scale/:nodeTemplateId', {}, {
      'scale': {
        method: 'POST'
      }
    });

    /* tags crud */
    // API REST Definition
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

    /*Users roles on an application*/
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

    var deploymentProperty = $resource('rest/applications/checkDeploymentProperty', {}, {
      'checkDeploymentProperty': {
        method: 'POST'
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
      'scale': ApplicationScalingDAO,
      'tags': applicationTags,
      'userRoles': manageAppUserRoles,
      'groupRoles': manageAppGroupRoles,
      'applicationStatus': applicationStatus,
      'checkProperty': deploymentProperty.checkDeploymentProperty,
    };
  }
]);