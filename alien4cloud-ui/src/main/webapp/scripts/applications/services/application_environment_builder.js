define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-applications').factory('appEnvironmentsBuilder', ['applicationEnvironmentServices', 'authService',
    function(applicationEnvironmentServices, authService) {

      var AppEnvironmentsPromise = function(application) {
        var instance = this;
        return applicationEnvironmentServices.getAllEnvironments(application.id).then(function(result) {
          var data = result.data.data;

          instance.environments = _.undefined(data) ? [] : data;

          instance.selected = null;
          if(_.isNotEmpty(data)){
            instance.selected =_.first(data);
            instance.selected.active = true;
          }

          // list of environments for which the user is a deployer
          instance.deployEnvironments=[];
          var isManager = authService.hasResourceRole(application, 'APPLICATION_MANAGER');
          _.each(instance.environments, function(environment){
            var isEnvDeployer = authService.hasResourceRole(environment, 'DEPLOYMENT_MANAGER');
            if (isManager || isEnvDeployer) {
              instance.deployEnvironments.push(environment);
            }
          });
          return instance;
        });


      };

      AppEnvironmentsPromise.prototype = {
        constructor: AppEnvironmentsPromise,

        //select an environment
        select: function(environmentId, envChangedCallback, force) {
          if(_.defined(this.selected)){
            if(this.selected.id === environmentId && !force) {
              return; // the environement is already selected.
            }
            this.selected.active = false;
          }
          for (var i = 0; i < this.environments.length; i++) {
            if (this.environments[i].id === environmentId) {
              this.selected = this.environments[i];
              this.selected.active = true;
              if(_.defined(envChangedCallback)) {
                envChangedCallback();
              }
            }
          }
        },

        // replace the environment with the one given as a parameter.
        updateEnvironment: function(environment) {
          var envIndex = _.findIndex(this.environments, 'id', environment.id);
          if (envIndex !== -1) { // note registration does not change
            this.environments.splice(envIndex, 1, environment);
          }
          envIndex = _.findIndex(this.deployEnvironments, 'id', environment.id);
          if (envIndex !== -1) {
            this.deployEnvironments.splice(envIndex, 1, environment);
          }
        }

      };

      return function(application) {
        return new AppEnvironmentsPromise(application).then(function(result){
          return result;
        });
      };
    }
  ]);
});
