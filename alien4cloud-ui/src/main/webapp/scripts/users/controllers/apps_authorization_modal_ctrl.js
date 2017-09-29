define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  require('scripts/applications/services/application_environment_services');
  require('scripts/common/services/search_service_factory');
  require('scripts/common/directives/pagination');

  modules.get('a4c-security', ['a4c-search']).controller('AppsAuthorizationModalCtrl', ['$scope', '$uibModalInstance', 'searchServiceFactory', 'applicationEnvironmentServices',
    function ($scope, $uibModalInstance, searchServiceFactory, applicationEnvironmentServices) {

      var generateEnvTypeId = function(app, envType) {
        return app.id + ':' + envType;
      };

      var buildPreselections = function buildPreselections (){
        $scope.preSelection =  {};
        $scope.preSelectedApps = {};
        $scope.preSelectedByType = {'envs': {}, 'envTypes': {}};
        $scope.envTypeList = applicationEnvironmentServices.environmentTypeList({}, {}, function() {});

        _.forEach($scope.authorizedSubjects, function(authorizedApp) {
          if (_.isEmpty(authorizedApp.environments)) {
            $scope.preSelectedApps[authorizedApp.application.id] = 1;
          }
          $scope.preSelection[authorizedApp.application.id] = {'envs': [], 'envTypes': []};
          _.forEach(authorizedApp.environments, function(environment) {
            $scope.preSelectedByType.envs[environment.id] = 1;
            $scope.preSelection[authorizedApp.application.id].envs.push(environment.id);
          });
          _.forEach(authorizedApp.environmentTypes, function(envType) {
            var envTypeFormated = generateEnvTypeId(authorizedApp.application, envType);
            $scope.preSelectedByType.envTypes[envTypeFormated] = 1;
            $scope.preSelection[authorizedApp.application.id].envTypes.push(envTypeFormated);
          });
        });
      };

      $scope._ = _;
      $scope.batchSize = 5;
      $scope.selectedApps = {};
      buildPreselections();

      // a map appId -> environment array
      $scope.environments = {};
      var onSearchCompleted = function (searchResult) {
        $scope.appsData = searchResult.data;
        _.forEach($scope.appsData.data, function(app) {
          if (app.id in $scope.preSelection) {
            $scope.selectedApps[app.id] = $scope.preSelection[app.id];
          }
        });
      };
      $scope.onSearchCompleted = onSearchCompleted;
      $scope.queryProvider = {
        query: '',
        onSearchCompleted: onSearchCompleted
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
        $scope.searchService = searchServiceFactory(url, useParams, $scope.queryProvider, $scope.batchSize, null, null, null, params);
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
        var result = { 'applicationsToDelete': [], 'environmentsToDelete': [], 'environmentTypesToDelete': [], 'applicationsToAdd': [], 'environmentsToAdd': [], 'environmentTypesToAdd': [] };
        _.forEach($scope.selectedApps, function(preSelectedByType, appId) {
          if (preSelectedByType.envs && preSelectedByType.envs.length > 0) {
            _.forEach(preSelectedByType.envs, function(env) {
              if (!(env in $scope.preSelectedByType.envs)) {
                result.environmentsToAdd.push(env);
              }
            });
          }
          if (preSelectedByType.envTypes && preSelectedByType.envTypes.length > 0) {
            _.forEach(preSelectedByType.envTypes, function(envType) {
              if (!(envType in $scope.preSelectedByType.envTypes)) {
                result.environmentTypesToAdd.push(envType);
              }
            });
          }
          if (!preSelectedByType.envs || !preSelectedByType.envTypes || (preSelectedByType.envs.length <= 0 && preSelectedByType.envTypes.length <= 0)) {
            result.applicationsToAdd.push(appId);
          }
        });

        _.forEach($scope.preSelectedApps, function(status, appId) {
          if (status === 0) {
            result.applicationsToDelete.push(appId);
          }
        });
        _.forEach($scope.preSelectedByType.envs, function(status, envId) {
          if (status === 0) {
            result.environmentsToDelete.push(envId);
          }
        });
        _.forEach($scope.preSelectedByType.envTypes, function(status, envType) {
          if (status === 0) {
            result.environmentTypesToDelete.push(envType);
          }
        });

        if (result.applicationsToDelete.length + result.environmentsToDelete.length + result.environmentTypesToDelete.length + result.applicationsToAdd.length + result.environmentsToAdd.length + result.environmentTypesToAdd.length > 0) {
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
          if ($scope.selectedApps[app.id].envs.length === 0 && $scope.selectedApps[app.id].envTypes.length === 0) {
            // no env or type for this app, toggle = no selection
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
          $scope.selectedApps[app.id] = {'envs': [], 'envTypes': []};
          if (app.id in $scope.preSelectedApps) {
            $scope.preSelectedApps[app.id] = 1;
          }
        }
      };

      /*
         0 : app not selected at all
         1 : partial selection (some env or type are selected)
         2 : the app itself is selected (means all environments)
      */
      $scope.getApplicationSelectionStatus = function (app) {
        if (app.id in $scope.selectedApps) {
          if ($scope.selectedApps[app.id].envs.length === 0 && $scope.selectedApps[app.id].envTypes.length === 0) {
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

      var toggleSelection = function (appId, value, type) {
        if (appId in $scope.preSelectedApps) {
          $scope.preSelectedApps[appId] = 0;
        }
        if (appId in $scope.selectedApps) {
          var indexOfEnv = $scope.selectedApps[appId][type].indexOf(value);
          if (indexOfEnv < 0) {
            $scope.selectedApps[appId][type].push(value);
            if (value in $scope.preSelectedByType[type]) {
              $scope.preSelectedByType[type][value] = 1;
            }
          } else {
            $scope.selectedApps[appId][type].splice(indexOfEnv, 1);
            if ($scope.selectedApps[appId].envs.length === 0 && $scope.selectedApps[appId].envTypes.length === 0) {
              delete $scope.selectedApps[appId];
            }
            if (value in $scope.preSelectedByType[type]) {
              $scope.preSelectedByType[type][value] = 0;
            }
          }
        } else {
          $scope.selectedApps[appId] = {'envs': [], 'envTypes': []};
          $scope.selectedApps[appId][type] = [ value ];
        }
      };
      $scope.toggleEnvironmentSelection = function (app, env) {
        toggleSelection(app.id, env.id, 'envs');
      };
      $scope.toggleEnvTypeSelection = function (app, envType) {
        toggleSelection(app.id, generateEnvTypeId(app, envType), 'envTypes');
      };

      var isSelected = function(appId, value, type) {
        if (appId in $scope.selectedApps && $scope.selectedApps[appId][type] && $scope.selectedApps[appId][type].indexOf(value) > -1) {
          return true;
        } else {
          return false;
        }
      };
      $scope.isEnvironmentSelected = function (app, env) {
        return isSelected(app.id, env.id, 'envs');
      };
      $scope.isEnvTypeSelected = function (app, envType) {
        return isSelected(app.id, generateEnvTypeId(app, envType), 'envTypes');
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
