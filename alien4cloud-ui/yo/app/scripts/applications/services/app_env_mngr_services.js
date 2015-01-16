/* global UTILS */

'use strict';

angular.module('alienUiApp').factory('appEnvMngrService', ['applicationEnvironmentServices',
function(applicationEnvironmentServices) {
  return {
    init: function(applicationId) {
      if(this.applicationId !== null) {
        this.close();
      }
      this.applicationId = applicationId;
      this.update();
    },
    update: function() {
      var instance = this;
      return applicationEnvironmentServices.getAllEnvironments(this.applicationId).then(function(result) {
        instance.environments = result.data.data;
        instance.updateRegistrations();
        return instance;
      });
    },
    updateRegistrations: function() {
      if(UTILS.isUndefinedOrNull(this.eventRegistrations)) {
        this.eventRegistrations = {};
      }
    },
    /**
    * Register for websocket updates on an environment status.
    */
    register: function(environment) {
      var registration = environmentEventServicesFactory(application.id, environment, callback);
      this.eventRegistrations[environment.id] = registration;
      var isEnvDeployer = alienAuthService.hasResourceRole(environment, 'DEPLOYMENT_MANAGER');
      if(isManager || isEnvDeployer) {
        this.deployEnvironments.push(environment);
      }
      isDeployer = isDeployer || isEnvDeployer;
    }
    close: function() {
      for(var i=0; i < this.eventRegistrations.length; i++) {
        this.eventRegistrations[i].close();
      }
      this.eventRegistrations = null;
      this.environments = null;
      this.deployEnvironments = null;
    }
  };
}]);
