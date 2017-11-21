define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  var angular = require('angular');

  modules.get('a4c-orchestrators').factory('locationResourcesSecurity', ['$resource', '$alresource',
    function ($resource, $alresource) {
      function createServiceInstance(baseUrl) {
        var users = $alresource(baseUrl+'/:resourceId/security/users/:username');

        var groups = $alresource(baseUrl+'/:resourceId/security/groups/:groupId');

        var applications = $alresource(baseUrl+'/:resourceId/security/applications/:applicationId');

        var environmentsPerApplication = $alresource(baseUrl+'/:resourceId/security/environmentsPerApplication');

        var bulkUsers = $resource(baseUrl+'/security/users', {}, {
          bulk: {
            method: 'POST',
            isArray: false,
            headers: {'Content-Type': 'application/json; charset=UTF-8'}
          }
        });

        var bulkGroups = $resource(baseUrl+'/security/groups', {}, {
          bulk: {
            method: 'POST',
            isArray: false,
            headers: {'Content-Type': 'application/json; charset=UTF-8'}
          }
        });

        var bulkEnvironmentsPerApplicationBatch = $resource(baseUrl+'/security/environmentsPerApplication', {}, {
          bulk: {
            method: 'POST',
            isArray: false,
            headers: {'Content-Type': 'application/json; charset=UTF-8'}
          }
        });

        return {
          'users': users,
          'groups': groups,
          'applications': applications,
          'environmentsPerApplication': environmentsPerApplication,
          'bulkUsers': bulkUsers.bulk,
          'bulkGroups': bulkGroups.bulk,
          'bulkEnvironmentsPerApplication': bulkEnvironmentsPerApplicationBatch.bulk
        };
      }

      return function(baseUrl, scope) {
        var service = createServiceInstance(baseUrl);

        /************************************
        *  For authorizations directives
        /************************************/

        var params = {
          orchestratorId: scope.context.orchestrator.id,
          locationId: scope.context.location.id
        };

        scope.buildSecuritySearchConfig = function(subject){
          return {
            url: 'rest/latest/orchestrators/:orchestratorId/locations/:locationId/security/' + subject + '/search',
            useParams: true,
            params: _.clone(params, true)
          };
        };

        // *****************************************************************************
        // USERS
        // *****************************************************************************

        scope.processUserAction = function (action, result) {
          var request = {
            'resources':  Object.keys(scope.context.selectedResourceTemplates)
          };
          request[action] = _.map(result.subjects, 'username');
          service.bulkUsers(_.merge(params, {force: result.force}), angular.toJson(request), function(successResponse) {
            console.log(successResponse);
            //TODO: check if an error occur and add a refresh
          });
        };

        // *****************************************************************************
        // GROUPS
        // *****************************************************************************

        scope.processGroupAction = function (action, result) {
          var request = {
            'resources':  Object.keys(scope.context.selectedResourceTemplates)
          };
          request[action] = _.map(result.subjects, 'id');
          service.bulkGroups(_.merge(params, {force:result.force}), angular.toJson(request), function(successResponse) {
            console.log(successResponse);
            //TODO: check if an error occur and add a refresh
          });
        };

        // *****************************************************************************
        // APPLICATIONS / ENVIRONMENTS
        // *****************************************************************************

        scope.processAppsAction = function (action, result) {
          var request = result.subjects;
          request.resources =  Object.keys(scope.context.selectedResourceTemplates);
          if (action === 'revoke') {
            request.applicationsToDelete = request.applicationsToAdd;
            request.environmentsToDelete = request.environmentsToAdd;
            request.environmentTypesToDelete = request.environmentTypesToAdd;
            delete request.applicationsToAdd;
            delete request.environmentsToAdd;
            delete request.environmentTypesToAdd;
          }
          service.bulkEnvironmentsPerApplication(_.merge(params, {force:result.force}), angular.toJson(request), function(successResponse) {
            console.log(successResponse);
            //TODO: check if an error occur and add a refresh
          });
        };

      };

    }]);
});
