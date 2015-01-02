/* global UTILS */
'use strict';

angular.module('alienUiApp').controller('ApplicationDeploymentCtrl', ['$scope', 'alienAuthService', '$upload', 'applicationServices', 'topologyServices',
  '$resource', '$http', '$q', '$translate', 'application', 'topologyId', 'environments', 'applicationEventServices', '$state', '$rootScope', 'applicationEnvironmentServices',
  function($scope, alienAuthService, $upload, applicationServices, topologyServices, $resource, $http, $q, $translate, applicationResult, topologyId, environments, applicationEventServices, $state, $rootScope, applicationEnvironmentServices) {
    var pageStateId = $state.current.name;
    $scope.application = applicationResult.data;

    $scope.topologyId = topologyId;
    $scope.isManager = alienAuthService.hasResourceRole($scope.application, 'APPLICATION_MANAGER');
    $scope.isDeployer = alienAuthService.hasResourceRole($scope.application, 'DEPLOYMENT_MANAGER');
    $scope.isDevops = alienAuthService.hasResourceRole($scope.application, 'APPLICATION_DEVOPS');
    $scope.isUser = alienAuthService.hasResourceRole($scope.application, 'APPLICATION_USER');

    $scope.outputAttributesValue = {};
    $scope.outputPropertiesValue = {};
    $scope.validTopologyDTO = false;

    var refreshSetupData = function() {
      $scope.selectedComputeTemplates = $scope.setup.cloudResourcesMapping;
      $scope.selectedNetworks = $scope.setup.networkMapping;
      $scope.deploymentProperties = $scope.setup.providerDeploymentProperties;
    };

    $scope.setCurrentMatchedComputeTemplates = function(name, currentMatchedComputeTemplates) {
      $scope.currentComputeNodeTemplateId = name;
      $scope.currentMatchedComputeTemplates = currentMatchedComputeTemplates;
    };

    $scope.setCurrentMatchedNetworks = function(name, currentMatchedNetworks) {
      $scope.currentNetworkNodeTemplateId = name;
      $scope.currentMatchedNetworks = currentMatchedNetworks;
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

    $scope.showProperty = function() {
      return !$scope.showTodoList() && UTILS.isDefinedAndNotNull($scope.deploymentPropertyDefinitions);
    };

    $scope.showTodoList = function() {
      return !$scope.validTopologyDTO.valid && $scope.isManager;
    };

    $scope.isSelectedCompute = function(template) {
      var selected = $scope.selectedComputeTemplates[$scope.currentComputeNodeTemplateId];
      return template.cloudImageId === selected.cloudImageId && template.cloudImageFlavorId === selected.cloudImageFlavorId;
    };

    $scope.isSelectedComputeTemplate = function(key) {
      return key === $scope.currentComputeNodeTemplateId;
    };

    $scope.isSelectedNetwork = function(template) {
      var selected = $scope.selectedNetworks[$scope.currentNetworkNodeTemplateId];
      return template.networkName === selected.networkName;
    };

    $scope.isSelectedNetworkName = function(key) {
      return key === $scope.currentNetworkNodeTemplateId;
    };

    $scope.isAllowedModify = function() {
      return UTILS.isDefinedAndNotNull($scope.topologyId) && ($scope.isManager || $scope.isDevops);
    };

    $scope.isAllowedInputDeployment = function() {
      return $scope.inputPropertiesSize > 0 && ($scope.isDeployer || $scope.isManager);
    };

    $scope.isAllowedDeployment = function() {
      return $scope.isDeployer || $scope.isManager;
    };

    // get the topology and it validity status
    if ($scope.topologyId) {
      topologyServices.dao.get({
        topologyId: $scope.topologyId
      }, function(result) {
        $scope.topologyDTO = result.data;
        if (UTILS.isDefinedAndNotNull($scope.topologyDTO.nodeTypes['tosca.nodes.Compute']) &&
          UTILS.isArrayDefinedAndNotEmpty($scope.topologyDTO.nodeTypes['tosca.nodes.Compute'].tags)) {
          $scope.computeImage = $scope.topologyDTO.nodeTypes['tosca.nodes.Compute'].tags[0].value;
        }
        if (UTILS.isDefinedAndNotNull($scope.topologyDTO.nodeTypes['tosca.nodes.Network']) &&
          UTILS.isArrayDefinedAndNotEmpty($scope.topologyDTO.nodeTypes['tosca.nodes.Network'].tags)) {
          $scope.networkImage = $scope.topologyDTO.nodeTypes['tosca.nodes.Network'].tags[0].value;
        }
        $scope.inputProperties = result.data.topology.inputProperties;
        $scope.outputProperties = result.data.topology.outputProperties;
        $scope.outputAttributes = result.data.topology.outputAttributes;
        $scope.inputArtifacts = result.data.topology.inputArtifacts;
        $scope.nodeTemplates = $scope.topologyDTO.topology.nodeTemplates;

        if (angular.isDefined(result.data.topology.inputProperties)) {
          $scope.inputPropertiesSize = Object.keys(result.data.topology.inputProperties).length;
        } else {
          $scope.inputPropertiesSize = 0;
        }

        if (angular.isDefined($scope.outputProperties)) {
          $scope.outputNodes = Object.keys($scope.outputProperties);
          $scope.outputPropertiesSize = Object.keys($scope.outputProperties).length;
          refreshOutputProperties();
        }

        if (angular.isDefined($scope.outputAttributes)) {
          $scope.outputNodes = UTILS.arrayUnique(UTILS.concat($scope.outputNodes, Object.keys($scope.outputAttributes)));
          $scope.outputAttributesSize = Object.keys($scope.outputAttributes).length;
        }

        if (angular.isDefined(result.data.topology.inputArtifacts)) {
          $scope.inputArtifactsSize = Object.keys(result.data.topology.inputArtifacts).length;
        } else {
          $scope.inputArtifactsSize = 0;
        }

        refreshCloudList();
        // Get the status
        refreshDeploymentStatus(false);
      });

      topologyServices.isValid({
        topologyId: $scope.topologyId
      }, function(result) {
        $scope.validTopologyDTO = result.data;
      });
    }

    var refreshOutputProperties = function() {
      for (var nodeId in $scope.outputProperties) {
        if ($scope.outputProperties.hasOwnProperty(nodeId)) {
          $scope.outputPropertiesValue[nodeId] = {};
          for (var i = 0; i < $scope.outputProperties[nodeId].length; i++) {
            var outputPropertyName = $scope.outputProperties[nodeId][i];
            $scope.outputPropertiesValue[nodeId][outputPropertyName] = $scope.nodeTemplates[nodeId].properties[outputPropertyName];
          }
        }
      }
    };

    var refreshOutputAttributes = function(appRuntimeInformation) {
      $scope.outputAttributesValue = {};
      applicationEventServices.subscribeToInstanceStateChange(pageStateId, onInstanceStateChange);
      if (UTILS.isDefinedAndNotNull(appRuntimeInformation)) {
        for (var nodeId in appRuntimeInformation) {
          if (appRuntimeInformation.hasOwnProperty(nodeId)) {
            $scope.outputAttributesValue[nodeId] = {};
            var nodeInformation = appRuntimeInformation[nodeId];
            for (var instanceId in nodeInformation) {
              if (nodeInformation.hasOwnProperty(instanceId)) {
                $scope.outputAttributesValue[nodeId][instanceId] = {};
                var allAttributes = nodeInformation[instanceId].attributes;
                for (var attribute in allAttributes) {
                  if (allAttributes.hasOwnProperty(attribute) && isOutput(nodeId, attribute, 'outputAttributes')) {
                    $scope.outputAttributesValue[nodeId][instanceId][attribute] = allAttributes[attribute];
                  }
                }
                if (Object.keys($scope.outputAttributesValue[nodeId][instanceId]).length === 0) {
                  delete $scope.outputAttributesValue[nodeId][instanceId];
                }
              }
            }
            var nbOfInstances = Object.keys($scope.outputAttributesValue[nodeId]).length;
            if (nbOfInstances === 0) {
              delete $scope.outputAttributesValue[nodeId];
            }
          }
        }
      }
    };

    var refreshInstancesStatuses = function() {
      if ($scope.outputAttributesSize > 0) {
        applicationServices.runtime.get({
          applicationId: $scope.application.id,
          applicationEnvironmentId: $scope.selectedEnvironment.id
        }, function(successResult) {
          refreshOutputAttributes(successResult.data);
        });
      }
    };

    var isOutput = function(nodeId, propertyName, type) {
      if (UTILS.isUndefinedOrNull($scope[type])) {
        return false;
      }
      if (!$scope[type].hasOwnProperty(nodeId)) {
        return false;
      }
      return $scope[type][nodeId].indexOf(propertyName) >= 0;
    };

    /**
     * Application status
     */
    var refreshDeploymentStatus = function(restart) {
      if (restart) {
        applicationEventServices.restart();
      }
      applicationEventServices.refreshApplicationStatus($scope.selectedEnvironment.id, function(newStatus) {
        $scope.deploymentStatus = newStatus;
        applicationEventServices.subscribeToStatusChange(pageStateId, onStatusChange);
        refreshInstancesStatuses();
      });
    };

    $rootScope.$on('REFRESH_DEPLOYMENT_STATUS', function refresh() {
      refreshDeploymentStatus(true);
      refreshDeploymentSetup();
      refreshCloudList();
    });

    var onInstanceStateChange = function(type, event) {
      if (UTILS.isUndefinedOrNull(event.instanceState)) {
        // Delete event
        if (UTILS.isDefinedAndNotNull($scope.outputAttributesValue[event.nodeTemplateId])) {
          delete $scope.outputAttributesValue[event.nodeTemplateId][event.instanceId];
          if (Object.keys($scope.outputAttributesValue[event.nodeTemplateId]).length === 0) {
            delete $scope.outputAttributesValue[event.nodeTemplateId];
          }
        }
      } else {
        // Add modify event
        var allAttributes = event.attributes;
        for (var attribute in allAttributes) {
          if (allAttributes.hasOwnProperty(attribute) && isOutput(event.nodeTemplateId, attribute, 'outputAttributes')) {
            if (UTILS.isUndefinedOrNull($scope.outputAttributesValue[event.nodeTemplateId])) {
              $scope.outputAttributesValue[event.nodeTemplateId] = {};
            }
            if (UTILS.isUndefinedOrNull($scope.outputAttributesValue[event.nodeTemplateId][event.instanceId])) {
              $scope.outputAttributesValue[event.nodeTemplateId][event.instanceId] = {};
            }
            $scope.outputAttributesValue[event.nodeTemplateId][event.instanceId][attribute] = allAttributes[attribute];
          }
        }
      }
      $scope.$apply();
    };

    var onStatusChange = function(type, event) {
      $scope.deploymentStatus = event.deploymentStatus;
      $scope.$apply();
    };

    $scope.$on('$destroy', function() {
      applicationEventServices.unsubscribeToStatusChange(pageStateId);
    });

    // Deployment handler
    $scope.deploy = function() {
      // Application details with deployment properties
      // TODO : deploy from environment
      var deployApplicationRequest = {
        applicationId: $scope.application.id,
        applicationEnvironmentId: $scope.selectedEnvironment.id
      };
      $scope.isDeploying = true;
      applicationServices.deployApplication.deploy([], angular.toJson(deployApplicationRequest), function() {
        $scope.deploymentStatus = 'DEPLOYMENT_IN_PROGRESS';
        //workaround
        //emit 'DEPLOYMENT_IN_PROGRESS' event, to change the runtime button status
        $rootScope.$emit('DEPLOYMENT_IN_PROGRESS');
        $scope.isDeploying = false;
        refreshDeploymentStatus(true);
      });
    };

    $scope.undeploy = function() {
      $scope.isUnDeploying = true;
      applicationServices.deployment.undeploy({
        applicationId: $scope.application.id,
        applicationEnvironmentId: $scope.selectedEnvironment.id
      }, function() {
        $scope.deploymentStatus = 'UNDEPLOYMENT_IN_PROGRESS';
        $scope.isUnDeploying = false;
      });
    };


    /* Handle properties inputs */
    $scope.updateProperty = function(nodeTemplateName, propertyName, propertyValue) {

      // No update if it's the same value
      if (propertyValue === $scope.nodeTemplates[nodeTemplateName].properties[propertyName]) {
        return;
      }
      var updatePropsObject = {
        'propertyName': propertyName,
        'propertyValue': propertyValue
      };

      var d = $q.defer();
      topologyServices.nodeTemplate.updateProperty({
        topologyId: $scope.topologyDTO.topology.id,
        nodeTemplateName: nodeTemplateName
      }, angular.toJson(updatePropsObject), function(data) {
        if (data.error !== null) {
          // Constraint error display + translation
          var constraintInfo = data.data;
          d.resolve($translate('ERRORS.' + data.error.code + '.' + constraintInfo.name, constraintInfo));
        } else {
          d.resolve();
          if (UTILS.isDefinedAndNotNull($scope.outputPropertiesValue[nodeTemplateName]) && UTILS.isDefinedAndNotNull($scope.outputPropertiesValue[nodeTemplateName][propertyName])) {
            $scope.outputPropertiesValue[nodeTemplateName][propertyName] = propertyValue;
          }
        }
      });
      return d.promise;
    };

    // Artifact upload handler
    $scope.doUploadArtifact = function(file, nodeTemplateName, artifactName) {
      if (UTILS.isUndefinedOrNull($scope.uploads)) {
        $scope.uploads = {};
      }
      $scope.uploads[artifactName] = {
        'isUploading': true,
        'type': 'info'
      };
      $upload.upload({
        url: 'rest/topologies/' + $scope.topologyDTO.topology.id + '/nodetemplates/' + nodeTemplateName + '/artifacts/' + artifactName,
        file: file
      }).progress(function(evt) {
        $scope.uploads[artifactName].uploadProgress = parseInt(100.0 * evt.loaded / evt.total);
      }).success(function(success) {
        $scope.nodeTemplates[nodeTemplateName].artifacts[artifactName].artifactRef = success.data;
        $scope.uploads[artifactName].isUploading = false;
        $scope.uploads[artifactName].type = 'success';
      }).error(function(data, status) {
        $scope.uploads[artifactName].type = 'error';
        $scope.uploads[artifactName].error = {};
        $scope.uploads[artifactName].error.code = status;
        $scope.uploads[artifactName].error.message = 'An Error has occurred on the server!';
      });
    };

    $scope.onArtifactSelected = function($files, nodeTemplateName, artifactName) {
      var file = $files[0];
      $scope.doUploadArtifact(file, nodeTemplateName, artifactName);
    };

    // DEPLOYMENT AND CLOUD MANAGEMENT

    var refreshDeploymentPropertyDefinitions = function() {
      if ($scope.selectedCloud) {
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
    };

    var refreshCloudResources = function() {
      if ($scope.selectedCloud && $scope.selectedEnvironment.hasOwnProperty('cloudId')) {
        delete $scope.currentMatchedComputeTemplates;
        applicationServices.matchResources({
          applicationId: $scope.application.id,
          applicationEnvironmentId: $scope.selectedEnvironment.id
        }, undefined, function(response) {
          $scope.matchedComputeResources = response.data.computeMatchResult;
          $scope.matchedNetworkResources = response.data.networkMatchResult;
          $scope.images = response.data.images;
          $scope.flavors = response.data.flavors;
          for (var key in $scope.matchedComputeResources) {
            if ($scope.matchedComputeResources.hasOwnProperty(key)) {
              if (!$scope.selectedComputeTemplates.hasOwnProperty(key)) {
                $scope.hasUnmatchedCompute = true;
                break;
              }
            }
          }
          for (var key in $scope.matchedNetworkResources) {
            if ($scope.matchedNetworkResources.hasOwnProperty(key)) {
              if (!$scope.selectedNetworks.hasOwnProperty(key)) {
                $scope.hasUnmatchedNetwork = true;
                break;
              }
            }
          }
        });
      }
    };

    var refreshDeploymentSetup = function() {
      applicationServices.getDeploymentSetup({
        applicationId: $scope.application.id,
        applicationEnvironmentId: $scope.selectedEnvironment.id
      }, undefined, function(response) {
        $scope.setup = response.data;
        refreshSetupData();
        refreshDeploymentPropertyDefinitions();
        refreshCloudResources();
      });
    };

    // search for clouds
    var Cloud = $resource('rest/clouds/search', {}, {});
    var refreshCloudList = function() {
      delete $scope.selectedCloud;
      delete $scope.deploymentPropertyDefinitions;
      Cloud.get({
        enabledOnly: true
      }, function(result) {
        var clouds = result.data.data;
        $scope.clouds = clouds;
        if (clouds) {
          // select the cloud that is currently associated with the topology
          var found = false,
            i = 0;
          while (!found && i < clouds.length) {
            if (clouds[i].id === $scope.selectedEnvironment.cloudId) {
              $scope.selectedCloud = clouds[i];
              refreshDeploymentSetup();
              found = true;
            }
            i++;
          }
        }
      });
    };

    // update the targeted cloud for the environment
    $scope.changeCloud = function(selectedCloud) {
      if (UTILS.isDefinedAndNotNull(selectedCloud)) {
        $scope.selectedComputeTemplates = {};
        $scope.selectedNetworks = {};
        var updateAppEnvRequest = {};
        updateAppEnvRequest.cloudId = selectedCloud.id;
        // update for the current environment
        applicationEnvironmentServices.update({
          applicationId: $scope.application.id,
          applicationEnvironmentId: $scope.selectedEnvironment.id
        }, angular.toJson(updateAppEnvRequest), function success(result) {
          $scope.selectedCloud = selectedCloud;
          refreshDeploymentStatus(true);
          refreshDeploymentSetup();
        });
      }
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
