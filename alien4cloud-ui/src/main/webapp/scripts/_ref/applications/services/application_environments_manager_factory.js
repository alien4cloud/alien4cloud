define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  require('scripts/applications/services/application_environment_services');
  require('scripts/applications/services/environment_event_services');

  modules.get('a4c-applications').factory('applicationEnvironmentsManagerFactory', ['$translate', 'toaster', 'applicationEnvironmentServices', 'environmentEventServicesFactory',
    function($translate, toaster, applicationEnvironmentServices, environmentEventServicesFactory) {

      var ApplicationEnvironmentsManager = function (application, environments) {
        this.application = application;
        this.environments = _.undefined(environments) ? [] : environments;

        this.eventRegistrations = [];
        for (var i = 0; i < this.environments.length; i++) {
          var environment = this.environments[i];
          this.registerEventListener(environment);
        }
      };

      ApplicationEnvironmentsManager.prototype = {
        constructor: ApplicationEnvironmentsManager,

        add: function(environment) {
          this.environments.push(environment);
          this.registerEventListener(environment);
        },

        // replace the environment with the one given as a parameter.
        update: function(environment) {
          var envIndex = _.findIndex(this.environments, 'id', environment.id);
          if (envIndex !== -1) { // note registration does not change
            this.environments.splice(envIndex, 1, environment);
          }
        },

        get: function(environmentId) {
          return _.find(this.environments, 'id', environmentId);
        },

        remove: function(environmentId) {
          var envIndex = _.findIndex(this.environments, 'id', environmentId);
          if (envIndex !== -1) { // remove the environment
            // close event registration
            this.eventRegistrations[envIndex].close();
            this.eventRegistrations.splice(envIndex, 1);
            this.environments.splice(envIndex, 1);
          }
        },

        stopEvents: function() {
          _.each(this.eventRegistrations, function(eventRegistration){
            eventRegistration.close();
          });
        },

        //reload the environment given its id
        reload: function(environmentId, optionalCallback) {
          var self = this;
          applicationEnvironmentServices.get({
            applicationId: this.application.id,
            applicationEnvironmentId: environmentId
          }, function(result){
            self.update(result.data);
            if (_.defined(optionalCallback)) {
              optionalCallback(result.data);
            }
          });
        },

        registerEventListener: function(environment) {
          var registration = environmentEventServicesFactory(this.application.id, environment, this.onDeploymentStatusEvent.bind(this));
          this.eventRegistrations.push(registration);
        },

        displayDeploymentStatusToaster: function(environment) {
          if (environment.status === 'FAILURE') {
            toaster.pop(
              'error',
              $translate.instant('DEPLOYMENT.STATUS.FAILURE'),
              $translate.instant('DEPLOYMENT.TOASTER_STATUS.FAILURE', {
                envName : environment.name,
                appName : this.application.name
              }),
              6000, 'trustedHtml', null
            );
          } else if (environment.status === 'DEPLOYED') {
            toaster.pop(
              'success',
              $translate.instant('DEPLOYMENT.STATUS.DEPLOYED'),
              $translate.instant('DEPLOYMENT.TOASTER_STATUS.DEPLOYED', {
                envName : environment.name,
                appName : this.application.name
              }),
              4000, 'trustedHtml', null
            );
          }
        },

        onDeploymentStatusEvent: function(environment, event) {
          environment.status = event.deploymentStatus;
          this.displayDeploymentStatusToaster(environment);
          if(_.defined(this.onEnvironmentStateChangedCallback)) {
            // Allow the application.detail controller to update the angular scope.
            this.onEnvironmentStateChangedCallback(environment);
          }
        },
      };

      // The application environments manager factory load all environments and return an environments manager instance
      return function(application) {
        // The factory returns a promise
        return applicationEnvironmentServices.getAllEnvironments(application.id).then(function(environmentSearchRestResponse) {
          // Once the promise is resolved we return an instance of the ApplicationEnvironmentsManager.
          return new ApplicationEnvironmentsManager(application, environmentSearchRestResponse.data.data);
        });
      };
    }
  ]);
});
