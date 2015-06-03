/* global UTILS, CONSTANTS */
'use strict';

angular.module('alienUiApp').controller('ApplicationDeploymentCtrl', ['$scope', 'alienAuthService', '$upload', 'applicationServices', 'topologyServices',
  '$resource', '$http', '$q', '$translate', 'application', '$state', '$rootScope', 'applicationEnvironmentServices', 'appEnvironments', 'toaster', '$timeout',
  function($scope, alienAuthService, $upload, applicationServices, topologyServices, $resource, $http, $q, $translate, applicationResult, $state, $rootScope, applicationEnvironmentServices, appEnvironments, toaster) {
    var pageStateId = $state.current.name;

    // We have to fetch the list of clouds in order to allow the deployment manager to change the cloud for the environment.
    var Cloud = $resource('rest/clouds/search', {}, {});

    // Initialization
    $scope.application = applicationResult.data;
    $scope.envs = appEnvironments.deployEnvironments;
    $scope.getResourceIcon = function(defaultImage, key) {
      if (UTILS.isDefinedAndNotNull($scope.topologyDTO) &&
        UTILS.isDefinedAndNotNull($scope.topologyDTO.topology) &&
        UTILS.isDefinedAndNotNull($scope.topologyDTO.topology.nodeTemplates) &&
        UTILS.isDefinedAndNotNull($scope.topologyDTO.topology.nodeTemplates[key])) {
        var tags = $scope.topologyDTO.nodeTypes[$scope.topologyDTO.topology.nodeTemplates[key].type].tags;
        if (UTILS.isDefinedAndNotNull(tags)) {
          var icon = UTILS.getIcon(tags);
          return 'img?id=' + (UTILS.isDefinedAndNotNull(icon) ? icon : defaultImage) + '&quality=QUALITY_64';
        } else {
          return null;
        }
      } else {
        return null;
      }
    };

    // set the environment to the given one and update the related data on screen.
    function setEnvironment(environment) {
      $scope.selectedEnvironment = environment;
      $scope.setTopologyId($scope.application.id, $scope.selectedEnvironment.id, checkTopology).$promise.then(function() {
        refreshDeploymentSetup();
      });
    }

    setEnvironment($scope.envs[0]); // default env

    function initializeCloudList() {
      Cloud.get({
        enabledOnly: true
      }, function(result) {
        var clouds = result.data.data;
        $scope.clouds = clouds;
      });
    }

    initializeCloudList();

    // update the configuration for the cloud
    function refreshDeploymentPropertyDefinitions() {
      if (!UTILS.isDefinedAndNotNull($scope.selectedCloud)) {
        return;
      }
      $http.get('rest/clouds/' + $scope.selectedCloud.id + '/deploymentpropertydefinitions').success(function(result) {
        if (result.data) {
          $scope.deploymentPropertyDefinitions = result.data;
          for (var propertyName in $scope.deploymentPropertyDefinitions) {
            if ($scope.deploymentPropertyDefinitions.hasOwnProperty(propertyName)) {
              $scope.deploymentPropertyDefinitions[propertyName].name = propertyName;
            }
          }
        }
      });
    }

    // refresh the actual selected cloud.
    function refreshSelectedCloud() {

      delete $scope.deploymentPropertyDefinitions;
      delete $scope.selectedCloud;

      // var clouds = $scope.clouds;
      if (UTILS.isDefinedAndNotNull($scope.clouds)) {
        // select the cloud that is currently associated with the environment
        var found = false,
          i = 0;
        while (!found && i < $scope.clouds.length) {
          if ($scope.clouds[i].id === $scope.selectedEnvironment.cloudId) {
            $scope.selectedCloud = $scope.clouds[i];
            found = true;
          }
          i++;
        }

        if (found) {
          refreshDeploymentPropertyDefinitions();
        } else {
          // No cloud rights or cloud not enabled or no loud defined
          if ($scope.selectedEnvironment.hasOwnProperty('cloudId')) {
            var errorTitle = $translate('APPLICATIONS.DEPLOYMENT.CLOUD_ERROR_TITLE');
            var errorMessage = $translate('APPLICATIONS.DEPLOYMENT.CLOUD_ERROR_MESSAGE');
            toaster.pop('error', errorTitle, errorMessage, 0, 'trustedHtml', null);
          }
        }
      }
    }

    // Retrieval and validation of the topology associated with the deployment.
    function checkTopology() {

      $scope.isTopologyValid($scope.topologyId, $scope.selectedEnvironment.id).$promise.then(function(validTopologyResult) {
        $scope.validTopologyDTO = validTopologyResult.data;
        prepareTasksAndWarnings($scope.validTopologyDTO);
      });

      var processTopologyInfoResult = $scope.processTopologyInformations($scope.topologyId);

      // when the selected environment is deployed => refresh output properties
      processTopologyInfoResult.$promise.then(function() {
        if ($scope.selectedEnvironment.status === 'DEPLOYED') {
          $scope.refreshInstancesStatuses($scope.application.id, $scope.selectedEnvironment.id, pageStateId);
        }
      });

    }

    // Just group tasks / warnings by category for the display
    function prepareTasksAndWarnings(validTopologyDTO) {
      // currently prepare warnings
      if (UTILS.isDefinedAndNotNull(validTopologyDTO.warningList)) {
        var preparedWarningList = {};
        validTopologyDTO.warningList.forEach(function(task) {
          if (!preparedWarningList.hasOwnProperty(task.code)) {
            preparedWarningList[task.code] = [];
          }
          preparedWarningList[task.code].push(task);
        });
        // replace the default warning list
        validTopologyDTO.warningList = preparedWarningList;
      }
    }

    // update the deployment configuration for the given environment.
    function refreshDeploymentSetup() {
      applicationServices.getDeploymentSetup({
        applicationId: $scope.application.id,
        applicationEnvironmentId: $scope.selectedEnvironment.id
      }, undefined, function(response) {
        $scope.hasUnmatchedCompute = false;
        delete $scope.currentMatchedStorages;
        delete $scope.currentMatchedNetworks;
        delete $scope.currentMatchedComputeTemplates;
        delete $scope.currentMatchedZones;
        $scope.setup = response.data;
        // update resource matching data.
        $scope.selectedComputeTemplates = $scope.setup.cloudResourcesMapping;
        $scope.selectedNetworks = $scope.setup.networkMapping;
        $scope.selectedStorages = $scope.setup.storageMapping;
        $scope.selectedZones = $scope.setup.availabilityZoneMapping;

        // update configuration of the PaaSProvider associated with the deployment setup
        $scope.deploymentProperties = $scope.setup.providerDeploymentProperties;
        refreshSelectedCloud();

        // only an environment with an associated cloud can have resources
        if (response.data.hasOwnProperty('matchResult')) {
          $scope.matchedComputeResources = response.data.matchResult.computeMatchResult;
          $scope.matchedNetworkResources = response.data.matchResult.networkMatchResult;
          $scope.matchedStorageResources = response.data.matchResult.storageMatchResult;
          $scope.matchedZoneResources = response.data.matchResult.availabilityZoneMatchResult;
          $scope.images = response.data.matchResult.images;
          $scope.flavors = response.data.matchResult.flavors;
        } else {
          delete $scope.matchedComputeResources;
          delete $scope.matchedNetworkResources;
          delete $scope.matchedStorageResources;
          delete $scope.matchedZoneResources;
          delete $scope.images;
          delete $scope.flavors;
          return;
        }

        $scope.hasUnmatchedCompute = hasUmatchedResource($scope.matchedComputeResources, $scope.selectedComputeTemplates);
        $scope.hasUnmatchedNetwork = hasUmatchedResource($scope.matchedNetworkResources, $scope.selectedNetworks);
        $scope.hasUnmatchedStorage = hasUmatchedResource($scope.matchedStorageResources, $scope.selectedStorages);
        checkGroupZonesAssociation();
      });
    }

    var checkGroupZonesAssociation = function() {
      // since we manage 50 HA policies, we need at least 2 zone per group
      // TODO: find a better way to express that
      $scope.hasUnmatchedGroup = hasUmatchedResource($scope.matchedZoneResources, $scope.selectedZones, CONSTANTS.minimumZoneCountPerGroup);
    };

    $scope.hasEnoughSelectedZones = function(groupId) {
      return $scope.selectedZones[groupId] && ($scope.selectedZones[groupId].length >= CONSTANTS.minimumZoneCountPerGroup);
    };

    var hasUmatchedResource = function(matchedResources, selectedResources, minimumSize) {
      var key;
      for (key in matchedResources) {
        if (matchedResources.hasOwnProperty(key) && !(selectedResources && selectedResources.hasOwnProperty(key))) {
          return true;
        }
        if (minimumSize !== undefined) {
          if (selectedResources[key].length < minimumSize) {
            return true;
          }
        }
      }
      return false;
    };

    // Change the selected environment (set only if required).
    var changeEnvironment = function(switchToEnvironment) {
      if (UTILS.isDefinedAndNotNull(switchToEnvironment) && switchToEnvironment.id !== $scope.selectedEnvironment.id) {
        setEnvironment(switchToEnvironment);
      }
    };

    var changeCloud = function(switchToCloud) {
      if (UTILS.isDefinedAndNotNull(switchToCloud)) {
        if (UTILS.isDefinedAndNotNull($scope.selectedCloud) && switchToCloud.id === $scope.selectedCloud.id) {
          return;
        }
        $scope.selectedComputeTemplates = {};
        $scope.selectedNetworks = {};
        $scope.selectedStorages = {};
        $scope.selectedZones = {};
        var updateAppEnvRequest = {};
        updateAppEnvRequest.cloudId = switchToCloud.id;
        // update for the current environment
        applicationEnvironmentServices.update({
          applicationId: $scope.application.id,
          applicationEnvironmentId: $scope.selectedEnvironment.id
        }, angular.toJson(updateAppEnvRequest), function success() {
          $scope.selectedCloud = switchToCloud;
          $scope.selectedEnvironment.cloudId = switchToCloud.id;
          refreshDeploymentSetup();
        });
      }
    };

    // Map functions that should be available from scope.
    $scope.changeEnvironment = changeEnvironment;
    // update the targeted cloud for the environment
    $scope.changeCloud = changeCloud;

    // Application rights
    $scope.isManager = alienAuthService.hasResourceRole($scope.application, 'APPLICATION_MANAGER');
    $scope.isDevops = alienAuthService.hasResourceRole($scope.application, 'APPLICATION_DEVOPS');
    // Application environment rights
    $scope.isDeployer = alienAuthService.hasResourceRole($scope.selectedEnvironment, 'DEPLOYMENT_MANAGER');
    $scope.isUser = alienAuthService.hasResourceRole($scope.selectedEnvironment, 'APPLICATION_USER');

    $scope.finalOutputAttributesValue = $scope.outputAttributesValue;
    $scope.validTopologyDTO = false;

    $scope.setCurrentMatchedComputeTemplates = function(name, currentMatchedComputeTemplates) {
      $scope.currentComputeNodeTemplateId = name;
      $scope.currentMatchedComputeTemplates = currentMatchedComputeTemplates;
    };

    $scope.setCurrentMatchedNetworks = function(name, currentMatchedNetworks) {
      $scope.currentNetworkNodeTemplateId = name;
      $scope.currentMatchedNetworks = currentMatchedNetworks;
    };

    $scope.setCurrentMatchedStorages = function(name, currentMatchedStorages) {
      $scope.currentStorageNodeTemplateId = name;
      $scope.currentMatchedStorages = currentMatchedStorages;
    };

    $scope.setCurrentGroup = function(name) {
      $scope.currentGroupId = name;
      $scope.currentMatchedZones = $scope.matchedZoneResources[name];
    };

    $scope.changeSelectedNetwork = function(template) {
      $scope.selectedNetworks[$scope.currentNetworkNodeTemplateId] = template;
      // Update deployment setup when matching change
      applicationServices.updateDeploymentSetup({
        applicationId: $scope.application.id,
        applicationEnvironmentId: $scope.selectedEnvironment.id
      }, angular.toJson({
        networkMapping: $scope.selectedNetworks
      }));
    };

    $scope.changeSelectedStorage = function(template) {
      $scope.selectedStorages[$scope.currentStorageNodeTemplateId] = template;
      // Update deployment setup when matching change
      applicationServices.updateDeploymentSetup({
        applicationId: $scope.application.id,
        applicationEnvironmentId: $scope.selectedEnvironment.id
      }, angular.toJson({
        storageMapping: $scope.selectedStorages
      }));
    };

    $scope.changeSelectedImage = function(template) {
      $scope.selectedComputeTemplates[$scope.currentComputeNodeTemplateId] = template;
      // Update deployment setup when matching change
      applicationServices.updateDeploymentSetup({
        applicationId: $scope.application.id,
        applicationEnvironmentId: $scope.selectedEnvironment.id
      }, angular.toJson({
        cloudResourcesMapping: $scope.selectedComputeTemplates
      }));
    };

    $scope.changeSelectedZone = function(zone) {
      var idx = UTILS.findByFieldValue($scope.selectedZones[$scope.currentGroupId], 'id', zone.id);
      if (idx < 0) {
        $scope.selectedZones[$scope.currentGroupId].push(zone);
      } else {
        $scope.selectedZones[$scope.currentGroupId].splice(idx, 1);
      }
      checkGroupZonesAssociation();
      // Update deployment setup when matching change
      applicationServices.updateDeploymentSetup({
        applicationId: $scope.application.id,
        applicationEnvironmentId: $scope.selectedEnvironment.id
      }, angular.toJson({
        availabilityZoneMapping: $scope.selectedZones
      }), function() {
        checkTopology();
      });
    };

    $scope.showProperty = function() {
      return !$scope.showTodoList() && UTILS.isDefinedAndNotNull($scope.deploymentPropertyDefinitions);
    };

    $scope.showTodoList = function() {
      return !$scope.validTopologyDTO.valid && $scope.isManager;
    };

    $scope.showWarningList = function() {
      return angular.isObject($scope.validTopologyDTO.warningList) && Object.keys($scope.validTopologyDTO.warningList).length > 0;
    };

    $scope.isSelectedCompute = function(template) {
      var selected = $scope.selectedComputeTemplates[$scope.currentComputeNodeTemplateId];
      return template.cloudImageId === selected.cloudImageId && template.cloudImageFlavorId === selected.cloudImageFlavorId && template.description === selected.description;
    };

    $scope.isSelectedComputeTemplate = function(key) {
      return key === $scope.currentComputeNodeTemplateId;
    };

    $scope.isSelectedNetwork = function(template) {
      var selected = $scope.selectedNetworks[$scope.currentNetworkNodeTemplateId];
      return UTILS.isDefinedAndNotNull(selected) && template.id === selected.id;
    };

    $scope.isSelectedStorage = function(template) {
      var selected = $scope.selectedStorages[$scope.currentStorageNodeTemplateId];
      return template.id === selected.id;
    };

    $scope.isSelectedNetworkName = function(key) {
      return key === $scope.currentNetworkNodeTemplateId;
    };

    $scope.isSelectedStorageName = function(key) {
      return key === $scope.currentStorageNodeTemplateId;
    };

    $scope.isSelectedGroup = function(key) {
      return key === $scope.currentGroupId;
    };

    $scope.isSelectedZone = function(zone) {
      return UTILS.findByFieldValue($scope.selectedZones[$scope.currentGroupId], 'id', zone.id) > -1;
    };

    $scope.isAllowedInputDeployment = function() {
      return $scope.inputsSize > 0 && ($scope.isDeployer || $scope.isManager);
    };

    $scope.isAllowedDeployment = function() {
      return $scope.isDeployer || $scope.isManager;
    };

    // if the status or the environment changes we must update the event registration.
    $scope.$watch(function(scope) {
      if (UTILS.isDefinedAndNotNull(scope.selectedEnvironment)) {
        return scope.selectedEnvironment.id + '__' + scope.selectedEnvironment.status;
      }
      return 'UNDEPLOYED';
    }, function(newValue) {
      var undeployedValue = $scope.selectedEnvironment.id + '__UNDEPLOYED';
      // no registration for this environement -> register if not undeployed!
      if (newValue === undeployedValue) {
        // if status the application is not undeployed we should register for events.
        $scope.stopEvent();
      } else {
        $scope.stopEvent();
        $scope.setTopologyId($scope.application.id, $scope.selectedEnvironment.id, checkTopology).$promise.then(function(result) {
          $scope.processTopologyInformations(result.data).$promise.then(function() {
            $scope.refreshInstancesStatuses($scope.application.id, $scope.selectedEnvironment.id, pageStateId);
          });
        });

      }
    });

    // when scope change, stop current event listener
    $scope.$on('$destroy', function() {
      $scope.stopEvent();
    });

    // Deployment handler
    $scope.deploy = function() {
      // Application details with deployment properties
      var deployApplicationRequest = {
        applicationId: $scope.application.id,
        applicationEnvironmentId: $scope.selectedEnvironment.id
      };
      $scope.isDeploying = true;
      applicationServices.deployApplication.deploy([], angular.toJson(deployApplicationRequest), function() {
        $scope.selectedEnvironment.status = 'DEPLOYMENT_IN_PROGRESS';
        $scope.isDeploying = false;
      }, function() {
        $scope.isDeploying = false;
      });
    };

    $scope.undeploy = function() {
      $scope.isUnDeploying = true;
      applicationServices.deployment.undeploy({
        applicationId: $scope.application.id,
        applicationEnvironmentId: $scope.selectedEnvironment.id
      }, function() {
        $scope.selectedEnvironment.status = 'UNDEPLOYMENT_IN_PROGRESS';
        $scope.isUnDeploying = false;
        $scope.stopEvent();
      });
    };

    /* Handle properties inputs */
    $scope.updateInputValue = function(definition, inputValue, inputId) {
      // No update if it's the same value
      if (UTILS.isUndefinedOrNull($scope.setup.inputProperties)) {
        $scope.setup.inputProperties = {};
      }
      if (inputValue === $scope.setup.inputProperties[inputId]) {
        return;
      } else {
        $scope.setup.inputProperties[inputId] = inputValue;
      }
      return applicationServices.updateDeploymentSetup({
        applicationId: $scope.application.id,
        applicationEnvironmentId: $scope.selectedEnvironment.id
      }, angular.toJson({
        inputProperties: $scope.setup.inputProperties
      }), function() {
        refreshDeploymentSetup();
        checkTopology();
      }).$promise;
    };

    // Artifact upload handler
    $scope.doUploadArtifact = function(file, artifactName) {
      if (UTILS.isUndefinedOrNull($scope.uploads)) {
        $scope.uploads = {};
      }
      $scope.uploads[artifactName] = {
        'isUploading': true,
        'type': 'info'
      };
      $upload.upload({
        url: 'rest/topologies/' + $scope.topologyDTO.topology.id + '/inputArtifacts/' + artifactName + '/upload',
        file: file
      }).progress(function(evt) {
        $scope.uploads[artifactName].uploadProgress = parseInt(100.0 * evt.loaded / evt.total);
      }).success(function(success) {
        $scope.inputArtifacts[artifactName].artifactRef = success.data.topology.inputArtifacts[artifactName].artifactRef;
        $scope.inputArtifacts[artifactName].artifactName = success.data.topology.inputArtifacts[artifactName].artifactName;
        $scope.uploads[artifactName].isUploading = false;
        $scope.uploads[artifactName].type = 'success';
      }).error(function(data, status) {
        $scope.uploads[artifactName].type = 'error';
        $scope.uploads[artifactName].error = {};
        $scope.uploads[artifactName].error.code = status;
        $scope.uploads[artifactName].error.message = 'An Error has occurred on the server!';
      });
    };

    $scope.onArtifactSelected = function($files, artifactName) {
      var file = $files[0];
      $scope.doUploadArtifact(file, artifactName);
    };

    /** Properties definition */
    $scope.updateDeploymentProperty = function(propertyDefinition, propertyValue) {
      var propertyName = propertyDefinition.name;
      if (propertyValue === $scope.deploymentProperties[propertyName]) {
        return; // no change
      }
      var deploymentPropertyObject = {
        'cloudId': $scope.selectedCloud.id,
        'deploymentPropertyName': propertyName,
        'deploymentPropertyValue': propertyValue
      };

      return applicationServices.checkProperty({}, angular.toJson(deploymentPropertyObject), function(data) {
        if (data.error === null) {
          $scope.deploymentProperties[propertyName] = propertyValue;
          // Update deployment setup when properties change
          applicationServices.updateDeploymentSetup({
            applicationId: $scope.application.id,
            applicationEnvironmentId: $scope.selectedEnvironment.id
          }, angular.toJson({
            providerDeploymentProperties: $scope.deploymentProperties
          }));
        }
      }).$promise;
    };
  }
]);
