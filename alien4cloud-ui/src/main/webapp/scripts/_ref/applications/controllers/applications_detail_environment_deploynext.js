define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');
  var angular = require('angular');

  require('scripts/_ref/applications/controllers/applications_detail_environment_deploynext_version');
  require('scripts/_ref/applications/controllers/applications_detail_environment_deploynext_topology');
  require('scripts/_ref/applications/controllers/applications_detail_environment_deploynext_inputs');
  require('scripts/_ref/applications/controllers/applications_detail_environment_deploynext_locations');
  require('scripts/_ref/applications/controllers/applications_detail_environment_deploynext_matching');
  require('scripts/_ref/applications/controllers/applications_detail_environment_deploynext_deploy');

  require('scripts/_ref/applications/services/environments_git_service');

  require('scripts/applications/services/deployment_topology_services');
  require('scripts/applications/services/deployment_topology_processor');
  require('scripts/applications/services/tasks_processor');
  require('scripts/applications/services/locations_matching_services');

  states.state('applications.detail.environment.deploynext', {
    url: '/deploy_next',
    templateUrl: 'views/_ref/applications/applications_detail_environment_deploynext.html',
    controller: 'ApplicationEnvDeployNextCtrl',
    menu: {
      id: 'applications.detail.environment.deploynext',
      state: 'applications.detail.environment.deploynext',
      key: 'NAVAPPLICATIONS.MENU_DEPLOY_NEXT',
      icon: '',
      priority: 100
    },
    resolve: {
      deploymentTopologyDTO: ['$stateParams', 'deploymentTopologyServices', 'deploymentTopologyProcessor', 'tasksProcessor',
        function ($stateParams, deploymentTopologyServices, deploymentTopologyProcessor, tasksProcessor) {
          return _.catch(function () {
            return deploymentTopologyServices.get({
              appId: $stateParams.id,
              envId: $stateParams.environmentId
            }).$promise.then(function (response) {
              var deploymentTopologyDTO = response.data;
              deploymentTopologyProcessor.process(deploymentTopologyDTO);
              tasksProcessor.processAll(deploymentTopologyDTO.validation);
              return deploymentTopologyDTO;
            });
          });
        }]
    }
  });

  var EditGitByEnvCtrl = ['$scope', '$uibModalInstance', 'environmentsGitService', 'gitLocation', 'environmentId',
    function($scope, $uibModalInstance, environmentsGitService, gitLocation, environmentId) {

      $scope.originalGitLocation = gitLocation;
      $scope.gitLocation = _.cloneDeep(gitLocation);
      if($scope.gitLocation.local) {
        $scope.gitLocation.url = 'http://';
      }

      $scope.save = function(valid) {
        if (valid && !_.isEqual($scope.originalGitLocation, $scope.gitLocation)) {
          if($scope.gitLocation.local) {
            environmentsGitService.delete({
              applicationId: $scope.application.id,
              environmentId : environmentId
            });
          } else {
            var request = {
              url: $scope.gitLocation.url,
              username: _.get($scope.gitLocation, 'credential.username'),
              password: _.get($scope.gitLocation, 'credential.password'),
              path: $scope.gitLocation.path,
              branch: $scope.gitLocation.branch
            };

            environmentsGitService.create({
              applicationId: $scope.application.id,
              environmentId : environmentId
            }, angular.toJson(request));
          }
          $uibModalInstance.close($scope.gitLocation);
        }
      };

      $scope.cancel = function() {
        $uibModalInstance.dismiss('cancel');
      };
    }
  ];

  modules.get('a4c-applications').controller('ApplicationEnvDeployNextCtrl',
    ['$scope', '$uibModal', '$state', 'menu', 'deploymentTopologyDTO', 'deploymentTopologyProcessor', 'tasksProcessor', 'locationsMatchingServices', 'deploymentServices',
    function ($scope, $uibModal, $state, menu, deploymentTopologyDTO, deploymentTopologyProcessor, tasksProcessor, locationsMatchingServices, deploymentServices) {
      $scope.deploymentTopologyDTO = deploymentTopologyDTO;

      $scope.$watch('environment.status', function (envStatus) {
        if (_.includes(['DEPLOYED', 'UPDATED'], envStatus)) {
          deploymentServices.runtime.getTopology({
            applicationId: $scope.application.id,
            applicationEnvironmentId: $scope.environment.id
          }).$promise.then(function (response) {
            $scope.deployedTopology = response.data;
          });
        }else{
          $scope.deployedTopology = null;
        }
      });

      // Initialize menu by setting next step property.
      for(var i=0; i<menu.length-1; i++) {
        menu[i].nextStep = menu[i+1];
      }
      $scope.menu = menu;

      var isLocationStepEnabled = false;
      // Fetch location matches information that are not in the deploymentTopologyDTO
      function initLocationMatches() {
        // If the location step is enabled fetch location matches
        if(isLocationStepEnabled) {
          locationsMatchingServices.getLocationsMatches({topologyId: deploymentTopologyDTO.topology.id, environmentId: $scope.environment.id}, function(result) {
            locationsMatchingServices.processLocationMatches($scope, result.data);
          });
        }
      }

      // INITIALIZE the selected menu to the first invalid or latest step
      var goToNextInvalidStep = function() {
        //menus are sorted by priority. first step is the top one
        var stepToGo = $scope.menu[0];

        //look for the first invalid step, or the last one if all are valid
        while(stepToGo.nextStep){
          if(_.get(stepToGo, 'step.status', 'SUCCESS') !== 'SUCCESS'){
            break;
          }
          stepToGo = stepToGo.nextStep;
        }

        //go to the found step
        $state.go(stepToGo.state);
      };

      function updateStepsStatuses(menu, validationDTO) {
        // set the status of each menu, based on the defined taskCodes and their presence in the validationDTO
        isLocationStepEnabled = false;
        var nextDisabled = false;
        _.each(menu, function(menuItem) {
          if(_.definedPath(menuItem, 'step.taskCodes')) {
            delete menuItem.step.status;
            menuItem.disabled = false;
            if(nextDisabled) {
              menuItem.disabled = true;
              return;
            }
            menuItem.step.status = 'SUCCESS';
            _.each(menuItem.step.taskCodes, function(taskCode) {
              if(_.definedPath(validationDTO, 'taskList['+taskCode+']')) {
                if(_.defined(menuItem.step.source)) {
                  var source = _.get(validationDTO, ['taskList', taskCode, 0, 'source']);
                  if(menuItem.step.source === source) {
                    menuItem.step.status = 'ERROR';
                    nextDisabled = true;
                    return false; // stop the _.each
                  }
                } else {
                  menuItem.step.status = 'ERROR';
                  nextDisabled = true;
                  return false; // stop the _.each
                }
              }
            });
          }
          if(menuItem.state === 'applications.detail.environment.deploynext.locations') {
            isLocationStepEnabled = !menuItem.disabled;
          }
        });
        initLocationMatches();
        // TODO Change only if the previous selected was the last
      }

      updateStepsStatuses(menu, deploymentTopologyDTO.validation);

      $scope.updateScopeDeploymentTopologyDTO = function(deploymentTopologyDTO) {
        if(_.undefined(deploymentTopologyDTO)){
          return;
        }
        deploymentTopologyProcessor.process(deploymentTopologyDTO);
        tasksProcessor.processAll(deploymentTopologyDTO.validation);

        $scope.deploymentTopologyDTO = deploymentTopologyDTO;
        updateStepsStatuses(menu, deploymentTopologyDTO.validation);
      };

      goToNextInvalidStep();

      // Modal to configure a custom git
      $scope.editGit = function(environmentId) {
        $uibModal.open({
          templateUrl: 'views/_ref/applications/applications_detail_environments_git.html',
          controller: EditGitByEnvCtrl,
          scope: $scope,
          resolve: {
            gitLocation: ['environmentsGitService', function (environmentsGitService) {
              return _.catch(function () {
                return environmentsGitService.get({
                  applicationId: $scope.application.id,
                  environmentId: environmentId
                }).$promise.then(function (result) {
                  return result.data;
                });
              });
            }],

            environmentId: function() {
              return environmentId;
            },
          }
        });
      };
    }
  ]);
});
