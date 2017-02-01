define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-applications').factory('appEnvironmentsBuilder', ['applicationEnvironmentServices', 'authService',
    function(applicationEnvironmentServices, authService) {

      var AppEnvironmentsPromise = function(application, environmentId) {
        var instance = this;
        return applicationEnvironmentServices.getAllEnvironments(application.id).then(function(result) {
          var data = result.data.data;
          instance.environments = _.undefined(data) ? [] : data;

          instance.selected = null;
          if(_.isNotEmpty(data)){
            if(_.defined(environmentId)) {
              instance.select(environmentId);
              if(_.undefined(instance.selected)) {
                // if environment id is not part of the list let's select first
                instance.selected =_.first(data);
              }
            } else {
              instance.selected =_.first(data);
            }
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

        /**
        * Performs selection of an environment based on it's id.
        *
        * @param environemnt The environment to actually select.
        * @param envChangedCallback An optional callback to be triggered once the environment has been selected.
        * @param force Update the selected value even if the selected environment is the provided one. This ensures that the callback will be called and that angularjs will re-digest the scope.
        */
        select: function(environmentId, envChangedCallback, force) {
          if(_.defined(this.selected)){
            if(this.selected.id === environmentId && !force) {
              return; // the environement is already selected.
            }
            this.selected.active = false;
          }
          for (var i = 0; i < this.environments.length; i++) {
            if (this.environments[i].id === environmentId) {
              this.doSelect(this.environments[i], envChangedCallback);
              return;
            }
          }
        },

        /**
        * Ensures that the current selected environemnt is deployed. If not selects the first available environment.
        * Note that if no environment is deployed the selection will be kept as is meaning a non-deployed environment will still be selected.
        */
        selectDeployed: function() {
          if(this.selected.status === 'UNDEPLOYED') {
            // select the first deployed environment
            for (var i = 0; i < this.environments.length; i++) {
              if (this.environments[i].status !== 'UNDEPLOYED') {
                this.doSelect(this.environments[i]);
                return;
              }
            }
          }
        },

        /**
        * Actually performs selection of a given environemnt.
        *
        * @param environemnt The environment to actually select.
        * @param envChangedCallback An optional callback to be triggered once the environment has been selected.
        */
        doSelect: function(environment, envChangedCallback) {
          this.selected = environment;
          this.selected.active = true;
          if(_.defined(envChangedCallback)) {
            envChangedCallback();
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

      return function(application, environmentId) {
        return new AppEnvironmentsPromise(application, environmentId).then(function(result){
          return result;
        });
      };
    }
  ]);
});
