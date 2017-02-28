define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  require('scripts/applications/services/application_environment_services');
  require('scripts/common/services/search_service_factory');
  require('scripts/common/directives/pagination');

  modules.get('a4c-security', ['a4c-search']).controller('AppsAuthorizationModalCtrl', ['$scope', '$uibModalInstance', 'searchServiceFactory', 'applicationEnvironmentServices',
    function ($scope, $uibModalInstance, searchServiceFactory, applicationEnvironmentServices) {

      var buildPreselections = function buildPreselections (){
        $scope.preSelection =  {};
        $scope.preSelectedApps = {};
        $scope.preSelectedEnvs= {};

        _.forEach($scope.authorizedSubjects, function(authorizedApp) {
          if (_.isEmpty(authorizedApp.environments)) {
            $scope.preSelectedApps[authorizedApp.application.id] = 1;
          }
          $scope.preSelection[authorizedApp.application.id] = [];
          _.forEach(authorizedApp.environments, function(environment) {
            $scope.preSelectedEnvs[environment.id] = 1;
            $scope.preSelection[authorizedApp.application.id].push(environment.id);
          });

        });

      };

      $scope._ = _;
      $scope.query = '';
      $scope.batchSize = 5;
      $scope.selectedApps = {};
      buildPreselections();

      // a map appId -> environment array
      $scope.environments = {};
      $scope.onSearchCompleted = function (searchResult) {
        $scope.appsData = searchResult.data;
        _.forEach($scope.appsData.data, function(app) {
            if (app.id in $scope.preSelection) {
                $scope.selectedApps[app.id] = $scope.preSelection[app.id];
            }
        });
      };

      $scope.expandEnvironments = function(app, isEnvironmentsCollapsed) {
        if (isEnvironmentsCollapsed) {
          return;
        }
        if (!$scope.environments[app.id]) {
          applicationEnvironmentServices.getAllEnvironments(app.id).then(function(result) {
            var data = result.data.data;
            $scope.environments[app.id] = data;
          });
        }
      };

      var buildSeachService = function(){
        var url;
        var useParams ;
        var params ;
        var searchConfig = _.isFunction($scope.buildSearchConfig) ? $scope.buildSearchConfig() : null;
        if($scope.customSearchActive) {
          url = _.get(searchConfig, 'url', '/rest/latest/applications/search');
          useParams = _.get(searchConfig, 'useParams', false);
          params = _.get(searchConfig, 'params', null);
        }else {
          url = '/rest/latest/applications/search';
          useParams = false;
          params = null;
        }
        $scope.searchService = searchServiceFactory(url, useParams, $scope, $scope.batchSize, null, null, null, params);
        $scope.searchService.search();
      };

      if (_.undefined($scope.application)) {
        $scope.editionMode = false;
        buildSeachService();
      } else {
        $scope.editionMode = true;
        $scope.onSearchCompleted({ 'data': { 'data': [$scope.application] }});
        $scope.expandEnvironments($scope.application, false);
      }

      $scope.ok = function () {
        var result = { 'applicationsToDelete': [], 'environmentsToDelete': [], 'applicationsToAdd': [], 'environmentsToAdd': [] };
        _.forEach($scope.selectedApps, function(envs, appId) {
          if (envs.length > 0) {
            _.forEach(envs, function(env) {
              if (!(env in $scope.preSelectedEnvs)) {
                result.environmentsToAdd.push(env);
              }
            });
          } else {
            if (!(appId in $scope.preSelectedApps)) {
              result.applicationsToAdd.push(appId);
            }
          }
        });
        _.forEach($scope.preSelectedApps, function(status, appId) {
          if (status === 0) {
            result.applicationsToDelete.push(appId);
          }
        });
        _.forEach($scope.preSelectedEnvs, function(status, envId) {
          if (status === 0) {
            result.environmentsToDelete.push(envId);
          }
        });

        if (result.applicationsToDelete.length + result.environmentsToDelete.length + result.applicationsToAdd.length + result.environmentsToAdd.length > 0) {
          $uibModalInstance.close({subjects: result});
        }
      };

      $scope.searchApp = function ($event) {
        $scope.selectedApps = {};
        $scope.environments = {};
        $scope.searchService.search();
        if(_.defined($event)){
          $event.preventDefault();
        }
      };

      $scope.toggleApplicationSelection = function (app) {
        if (app.id in $scope.selectedApps) {
          if ($scope.selectedApps[app.id].length === 0) {
            // no env for this app, toggle = no selection
            delete $scope.selectedApps[app.id];
            // app was previoulsy selected (or partially selected)
            if (app.id in $scope.preSelectedApps) {
              $scope.preSelectedApps[app.id] = 0;
            }
          } else {
            // some env exist, toggle = full selection
            $scope.selectedApps[app.id] = [];
            if (app.id in $scope.preSelectedApps) {
              $scope.preSelectedApps[app.id] = 1;
            }
          }
        } else {
          $scope.selectedApps[app.id] = [];
          if (app.id in $scope.preSelectedApps) {
            $scope.preSelectedApps[app.id] = 1;
          }
        }
      };

      /*
         0 : app not selected at all
         1 : partial selection (some env are selected)
         2 : the app itself is selected (means all environments)
      */
      $scope.getApplicationSelectionStatus = function (app) {
        if (app.id in $scope.selectedApps) {
          if ($scope.selectedApps[app.id].length === 0) {
            // no env for this app, full selection
            return 2;
          } else {
            // env selected for this app, partial selection
            return 1;
          }
        } else {
          return 0;
        }
      };

      $scope.toggleEnvironmentSelection = function (app, env) {
        if (app.id in $scope.preSelectedApps) {
          $scope.preSelectedApps[app.id] = 0;
        }
        if (app.id in $scope.selectedApps) {
          var indexOfEnv = $scope.selectedApps[app.id].indexOf(env.id);
          if (indexOfEnv < 0) {
            $scope.selectedApps[app.id].push(env.id);
            if (env.id in $scope.preSelectedEnvs) {
              $scope.preSelectedEnvs[env.id] = 1;
            }
          } else {
            $scope.selectedApps[app.id].splice(indexOfEnv, 1);
            if ($scope.selectedApps[app.id].length === 0) {
                delete $scope.selectedApps[app.id];
            }
            if (env.id in $scope.preSelectedEnvs) {
              $scope.preSelectedEnvs[env.id] = 0;
            }
          }
        } else {
          $scope.selectedApps[app.id] = [ env.id ];
        }
      };

      $scope.isEnvironmentSelected = function (app, env) {
        if (app.id in $scope.selectedApps && $scope.selectedApps[app.id].indexOf(env.id) > -1) {
          return true;
        } else {
          return false;
        }
      };

      $scope.toggleSelectAll = function () {
        var selectAllStatus = $scope.getSelectAllStatus();
        switch(selectAllStatus) {
          case 1:
            _.forEach($scope.appsData.data, function(app) {
              var appSelectionStatus = $scope.getApplicationSelectionStatus(app);
              if (appSelectionStatus === 0) {
                $scope.toggleApplicationSelection(app);
              }
            });
            break;
          case 0:
          case 2:
            _.forEach($scope.appsData.data, function(app) {
              $scope.toggleApplicationSelection(app);
            });
            break;
          case -1:
            // we don't authorize select all when partial selection
            return;
        }
      };

      /*
        -1 : at least 1 app is partially selected (env selected)
         0 : no app selected at all
         1 : partial fully selection (some apps are fully selected, others are not)
         2 : all apps are fully selected
      */
      $scope.getSelectAllStatus = function () {
        var result = 0;
        _.forEach($scope.appsData.data, function(app) {
          if (result === -1) {
            return;
          }
          var appSelectionStatus = $scope.getApplicationSelectionStatus(app);
          if (appSelectionStatus === 1) {
            result = -1;
          } else {
            result += appSelectionStatus;
          }
        });
        switch(result) {
          case -1:
            return -1;
          case 0:
            // no app is selected
            return 0;
          case $scope.appsData.data.length * 2:
            // all apps are fully selected
            return 2;
          default:
            // some app are fully selected, others not
            return 1;
        }
      };

      $scope.toggleCustomSearch = function(){
        $scope.customSearchActive = !$scope.customSearchActive;
        $scope.emptyPlaceHolder = $scope.customSearchActive ? 'ORCHESTRATORS.LOCATIONS.AUTHORIZATIONS.APPS.ADD_POPUP.EMPTY_PLACEHOLDER' : 'APPLICATIONS.APPLICATION';
        buildSeachService();
      };

      $scope.cancel = function () {
        $uibModalInstance.dismiss('cancel');
      };

    }
  ]);
});
