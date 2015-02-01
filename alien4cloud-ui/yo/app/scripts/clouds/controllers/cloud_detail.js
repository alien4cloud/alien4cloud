/* global UTILS */
'use strict';

angular.module('alienUiApp').controller(
  'CloudDetailController', ['$scope', '$http', '$resource', '$stateParams', '$timeout', 'cloudServices', '$state', 'deploymentServices', 'toaster', '$translate', 'userServices', 'groupServices', '$modal', 'resizeServices', '$q', 'searchServiceFactory', 'cloudImageServices',
    function($scope, $http, $resource, $stateParams, $timeout, cloudServices, $state, deploymentServices, toaster, $translate, userServices, groupServices, $modal, resizeServices, $q, searchServiceFactory, cloudImageServices) {
      var cloudId = $stateParams.id;

      $scope.iaasTypes = ['OTHER', 'AZURE', 'OPENSTACK', 'VMWARE', 'AMAZON', 'VIRTUALBOX'];
      $scope.envTypes = ['OTHER', 'DEVELOPMENT', 'INTEGRATION_TESTS', 'USER_ACCEPTANCE_TESTS', 'PRE_PRODUCTION', 'PRODUCTION'];
      $scope.tabs = {
        newTemplates: 0
      };
      /**
       * FOR USER SEARCH AND ADD GROUP'S ROLE
       */
      var updateRoles = function(roles, role, operation) {
        switch (operation) {
          case 'add':
            if (!roles) {
              roles = [];
            }
            roles.push(role);
            return roles;
          case 'remove':
            var index = roles.indexOf(role);
            roles.splice(index, 1);
            return roles;

          default:
            break;
        }
      };

      var updateComputeResourcesId = function() {
        if ($scope.manualMatchResource) {
          var templateLength = $scope.matchedComputeTemplates.length;
          for (var i = 0; i < templateLength; i++) {
            var matched = $scope.matchedComputeTemplates[i].computeTemplate;
            var originalIndex = UTILS.findByFieldValues($scope.cloud.computeTemplates, {
              cloudImageId: matched.cloudImageId,
              cloudImageFlavorId: matched.cloudImageFlavorId
            });
            var original = $scope.cloud.computeTemplates[originalIndex];
            original.paaSResourceId = $scope.matchedComputeTemplates[i].paaSResourceId;
          }
        }
      };

//      var updateNetworkResourcesId = function() {
//        if ($scope.manualMatchResource) {
//          var networksLength = $scope.matchedNetworks.length;
//          for (var i = 0; i < networksLength; i++) {
//            var matched = $scope.matchedNetworks[i].network;
//            var originalIndex = UTILS.findByFieldValues($scope.cloud.networks, {
//              networkName: matched.networkName
//            });
//            var original = $scope.cloud.networks[originalIndex];
//            original.paaSResourceId = $scope.matchedNetworks[i].paaSResourceId;
//          }
//        }
//      };

      var refreshCloud = function() {
        cloudServices.get({
          id: cloudId
        }, function(response) {
          // the cloud
          $scope.cloud = response.data.cloud;
          // templates computed by backend
          $scope.templates = response.data.cloud.computeTemplates;
          // stuff associated to the cloud
          $scope.images = response.data.images;
          $scope.flavors = response.data.flavors;
          $scope.networks = response.data.networks;
          $scope.storages = response.data.storages;
          // ids coming from pass
          $scope.paaSImageIds = response.data.paaSImageIds;
          $scope.paaSFlavorIds = response.data.paaSFlavorIds;
          $scope.paaSNetworkIds = response.data.paaSNetworkTemplateIds;
          $scope.paaSStorageIds = response.data.paaSStorageTemplateIds;    
          // array of PaaS stuff IDs available for mapping
          $scope.availaiblePaaSImageIds = [];
          $scope.availaiblePaaSFlavorIds = [];
          $scope.availaiblePaaSNetworkIds = [];
          $scope.availaiblePaaSStorageIds = [];
          // counters for non mapped stuffs
          $scope.imageNotConfiguredCount = 0;
          $scope.flavorNotConfiguredCount = 0;
          $scope.networkNotConfiguredCount = 0;
          $scope.storageNotConfiguredCount = 0;
          
          updateImageResourcesStatistic();
          updateFlavorResourcesStatistic();
          updateComputeResourcesStatistic();
          updateNetworkResourcesStatistic();
          updateStorageResourcesStatistic();
          
          $scope.relatedUsers = {};
          if ($scope.cloud.userRoles) {
            var usernames = [];
            for (var username in $scope.cloud.userRoles) {
              if ($scope.cloud.userRoles.hasOwnProperty(username)) {
                usernames.push(username);
              }
            }
            if (usernames.length > 0) {
              userServices.get([], angular.toJson(usernames), function(usersResults) {
                var data = usersResults.data;
                for (var i = 0; i < data.length; i++) {
                  $scope.relatedUsers[data[i].username] = data[i];
                }
              });
            }
          }

          $scope.relatedGroups = {};
          if ($scope.cloud.groupRoles) {
            var groupIds = [];
            for (var groupId in $scope.cloud.groupRoles) {
              if ($scope.cloud.groupRoles.hasOwnProperty(groupId)) {
                groupIds.push(groupId);
              }
            }
            if (groupIds.length > 0) {
              groupServices.getMultiple([], angular.toJson(groupIds), function(groupsResults) {
                var data = groupsResults.data;
                for (var i = 0; i < data.length; i++) {
                  $scope.relatedGroups[data[i].id] = data[i];
                }
              });
            }
          }
        });
      }

      refreshCloud();
      // get all cloud assignable roles
      $resource('rest/auth/roles/cloud', {}, {
        method: 'GET'
      }).get().$promise.then(function(roleResult) {
          $scope.cloudRoles = roleResult.data;
        });

      $scope.updateCloud = function(cloud) {

        cloud.id = $scope.cloud.id;
        $scope.cloudSaving = true;

        var resetSaved = function() {
          $scope.cloudSavedSuccess = false;
          $scope.cloudSavedError = false;
        };

        cloudServices.update([], angular.toJson(cloud), function() {
          $scope.cloudSaving = false;
          $scope.cloudSavedSuccess = true;
          $timeout(resetSaved, 500, true);
        }, function() {
          $scope.cloudSaving = false;
          $scope.cloudSavedError = true;
          $timeout(resetSaved, 500, true);
        });
      };

      $scope.enableCloud = function() {
        $scope.enablePending = true;
        $http.get('rest/clouds/' + cloudId + '/enable')
          .success(function(response) {
            if (UTILS.isDefinedAndNotNull(response.error)) {
              // toaster message
              toaster.pop('error', $translate('CLOUDS.ERRORS.ENABLING_FAILED_TITLE'), $translate('CLOUDS.ERRORS.ENABLING_FAILED'), 4000, 'trustedHtml', null);
              $scope.cloud.enabled = false;
            } else {
              refreshCloud();
              $scope.cloud.enabled = true;
            }
            $scope.enablePending = false;
          })
          .error(function() {
            $scope.enablePending = false;
          });
      };

      $scope.disableCloud = function() {
        $scope.enablePending = true;
        $http.get('rest/clouds/' + cloudId + '/disable')
          .success(function(response) {
            if (response.data) {
              $scope.cloud.enabled = false;
            } else {
              // toaster message
              toaster.pop('error', $translate('CLOUDS.ERRORS.DISABLING_FAILED_TITLE'), $translate('CLOUDS.ERRORS.DISABLING_FAILED'), 4000, 'trustedHtml', null);
            }
            $scope.enablePending = false;
          }).error(function() {
            $scope.enablePending = false;
          });
      };

      $scope.cloudConfig = {};

      cloudServices.config.get({
        id: cloudId
      }, function(response) {
        if (UTILS.isDefinedAndNotNull(response.data)) {
          $scope.cloudConfig = response.data;
        }
      });

      // get the configuration for the cloud.
      $http.get('rest/formdescriptor/cloudConfig/' + cloudId).success(function(result) {
        $scope.cloudConfigDefinition = result.data;
      });

      $scope.saveConfiguration = function(newConfiguration) {
        return cloudServices.config.update({
          id: cloudId
        }, angular.toJson(newConfiguration), function success(response) {
          $scope.cloudConfig = newConfiguration;
          if (UTILS.isDefinedAndNotNull(response.error)) {
            var errorsHandle = $q.defer();
            return errorsHandle.resolve(response.error);
          } else {
            refreshCloud();
          }
        }).$promise;
      };

      //delete a cloud
      $scope.removeCloud = function(cloudId) {
        cloudServices.remove({
          id: cloudId
        }, function(response) {
          if (response.data === true) {
            $state.go('admin.clouds.list');
          } else {
            // toaster message
            toaster.pop('error', $translate('CLOUDS.ERRORS.DELETING_FAILED_TITLE'), $translate('CLOUDS.ERRORS.DELETING_FAILED'), 4000, 'trustedHtml', null);
          }
        });
      };

      $scope.closeErrorAlert = function() {
        $scope.actionErrors = null;
      };

      //get all deployments for this cloud
      deploymentServices.get({
        cloudId: cloudId,
        includeAppSummary: true
      }, function(result) {
        $scope.deployments = result.data;
      });

      // Handle cloud security action
      $scope.handleRoleSelectionForUser = function(user, role) {
        if (UTILS.isUndefinedOrNull($scope.cloud.userRoles)) {
          $scope.cloud.userRoles = {};
        }
        var cloudUserRoles = $scope.cloud.userRoles[user.username];
        if (!cloudUserRoles || cloudUserRoles.indexOf(role) < 0) {

          cloudServices.userRoles.addUserRole([], {
            cloudId: $scope.cloud.id,
            username: user.username,
            role: role
          }, function() {
            $scope.cloud.userRoles[user.username] = updateRoles(cloudUserRoles, role, 'add');
            if (!$scope.relatedUsers[user.username]) {
              $scope.relatedUsers[user.username] = user;
            }
          });

        } else {
          cloudServices.userRoles.removeUserRole([], {
            cloudId: $scope.cloud.id,
            username: user.username,
            role: role
          }, function() {
            $scope.cloud.userRoles[user.username] = updateRoles(cloudUserRoles, role, 'remove');
          });
        }
      };

      $scope.handleRoleSelectionForGroup = function(group, role) {
        if (UTILS.isUndefinedOrNull($scope.cloud.groupRoles)) {
          $scope.cloud.groupRoles = {};
        }
        var cloudGroupRoles = $scope.cloud.groupRoles[group.id];

        if (!cloudGroupRoles || cloudGroupRoles.indexOf(role) < 0) {
          cloudServices.groupRoles.addGroupRole([], {
            cloudId: $scope.cloud.id,
            groupId: group.id,
            role: role
          }, function() {
            $scope.cloud.groupRoles[group.id] = updateRoles(cloudGroupRoles, role, 'add');
            if (!$scope.relatedGroups[group.id]) {
              $scope.relatedGroups[group.id] = group;
            }
          });

        } else {
          cloudServices.groupRoles.removeGroupRole([], {
            cloudId: $scope.cloud.id,
            groupId: group.id,
            role: role
          }, function() {
            $scope.cloud.groupRoles[group.id] = updateRoles(cloudGroupRoles, role, 'remove');
          });
        }
      };

      $scope.checkCloudRoleSelectedForUser = function(user, role) {
        if ($scope.cloud && $scope.cloud.userRoles && $scope.cloud.userRoles[user.username]) {
          return $scope.cloud.userRoles[user.username].indexOf(role) > -1;
        }
        return false;
      };

      $scope.checkCloudRoleSelectedForGroup = function(group, role) {
        if ($scope.cloud && $scope.cloud.groupRoles && $scope.cloud.groupRoles[group.id]) {
          return $scope.cloud.groupRoles[group.id].indexOf(role) > -1;
        }
        return false;
      };

      var updateNetworkResourcesStatistic = function() {
        var result = updateResourcesStatistic($scope.paaSNetworkIds, $scope.networks);
        $scope.networkNotConfiguredCount = result.counter;
        $scope.availaiblePaaSNetworkIds = result.arr;
      };

      var updateComputeResourcesStatistic = function() {
        $scope.templateActiveCount = 0;
        $scope.templateNotConfiguredCount = 0;
        for (var i = 0; i < $scope.cloud.computeTemplates.length; i++) {
          if ($scope.cloud.computeTemplates[i].enabled === true) {
            $scope.templateActiveCount++;
            if (UTILS.isUndefinedOrNull($scope.cloud.computeTemplates[i].paaSResourceId)) {
              $scope.templateNotConfiguredCount++;
            }
          }
        }
        $scope.templateFilteredCount = $scope.cloud.images.length * $scope.cloud.flavors.length - $scope.cloud.computeTemplates.length;
      };

      // generic fn that count the number of un-associated stuffs and define available PaaS IDs array
      var updateResourcesStatistic = function(paaSResourceIdArr, alienResourceMap) {
        var notConfiguredCount = 0;
        if (paaSResourceIdArr) {
          // clone the array
          var availablePaaSResourceIdArr = paaSResourceIdArr.slice(0); 
        }
        angular.forEach(alienResourceMap, function(value, key) {
          if (UTILS.isUndefinedOrNull(value.paaSResourceId)) {
            notConfiguredCount++;
          } else if (paaSResourceIdArr) {
            // this resource id is mapped, not available for others
            UTILS.arrayRemove(availablePaaSResourceIdArr, value.paaSResourceId);
          }          
        });
        return {counter: notConfiguredCount, arr: availablePaaSResourceIdArr}
      };
      
      // count the number of images that are not associated to a resource id
      var updateImageResourcesStatistic = function() {
        var result = updateResourcesStatistic($scope.paaSImageIds, $scope.images);
        $scope.imageNotConfiguredCount = result.counter;
        $scope.availaiblePaaSImageIds = result.arr;
      };
      
      // count the number of flavors that are not associated to a resource id
      var updateFlavorResourcesStatistic = function() {
        var result = updateResourcesStatistic($scope.paaSFlavorIds, $scope.flavors);
        $scope.flavorNotConfiguredCount = result.counter;
        $scope.availaiblePaaSFlavorIds = result.arr;
      };   
      
      // count the number of storages that are not associated to a resource id
      var updateStorageResourcesStatistic = function() {
        var result = updateResourcesStatistic($scope.paaSStorageIds, $scope.storages);
        $scope.storageNotConfiguredCount = result.counter;
        $scope.availaiblePaaSStorageIds = result.arr;
      };       
      
      var updateComputeResources = function(cloudResources) {
        var newComputeTemplates = cloudResources.computeTemplates;
        $scope.tabs.newTemplates = newComputeTemplates.length - $scope.cloud.computeTemplates.length;
        $scope.cloud.computeTemplates = newComputeTemplates;
        if (UTILS.isDefinedAndNotNull(cloudResources.matchedComputeTemplates)) {
          $scope.matchedComputeTemplates = cloudResources.matchedComputeTemplates;
        }
        updateComputeResourcesId();
        updateComputeResourcesStatistic();
      };

//      var updateNetworkResources = function() {
//        updateNetworkResourcesId();
//        updateNetworkResourcesStatistic();
//      };

      /** handle Modal form for cloud flavor creation */
      $scope.openFlavorCreationModal = function() {
        var modalInstance = $modal.open({
          templateUrl: 'views/clouds/new_flavor.html',
          controller: 'NewCloudImageFlavorController'
        });

        modalInstance.result.then(function(flavor) {
          cloudServices.addFlavor({
            id: $scope.cloud.id
          }, angular.toJson(flavor), function(success) {
            $scope.flavors[flavor.id] = {resource: flavor};
            $scope.cloud.flavors.push(flavor);
            updateFlavorResourcesStatistic();
//            updateComputeResources(success.data);
          });
        });
      };

      $scope.deleteFlavor = function(flavorId) {
        cloudServices.removeFlavor({
          id: $scope.cloud.id,
          flavorId: flavorId
        }, undefined, function(success) {
          var indexFlavor = UTILS.findByFieldValue($scope.cloud.flavors, 'id', flavorId);
          $scope.cloud.flavors.splice(indexFlavor, 1);
          delete $scope.flavors[flavorId];
          updateFlavorResourcesStatistic();
//          updateComputeResources(success.data);
        });
      };

      /** handle Modal form for cloud network creation */
      $scope.openNetworkCreationModal = function() {
        var modalInstance = $modal.open({
          templateUrl: 'views/clouds/new_network.html',
          controller: 'NewNetworkController'
        });

        modalInstance.result.then(function(network) {
          network.id = network.networkName;
          cloudServices.addNetwork({
            id: $scope.cloud.id
          }, angular.toJson(network), function() {
            $scope.networks[network.id] = {resource: network};
            $scope.cloud.networks.push(network);
            updateNetworkResourcesStatistic();
//            updateNetworkResources();
          });
        });
      };

      /** handle Modal form for cloud storage creation */
      $scope.openStorageCreationModal = function() {
        var modalInstance = $modal.open({
          templateUrl: 'views/clouds/new_storage.html',
          controller: 'NewStorageController'
        });

        modalInstance.result.then(function(storage) {
          cloudServices.addStorage({
            id: $scope.cloud.id
          }, angular.toJson(storage), function() {
            $scope.storages[storage.id] = {resource: storage};
            $scope.cloud.storages.push(storage);
            updateStorageResourcesStatistic();
//            updateNetworkResources();
          });
        });
      };
      
      $scope.deleteNetwork = function(networkName) {
        cloudServices.removeNetwork({
          id: $scope.cloud.id,
          networkName: networkName
        }, undefined, function(success) {
          delete $scope.networks[networkName];
          var indexOfNetwork = $scope.cloud.networks.indexOf(networkName);
          $scope.cloud.networks.splice(indexOfNetwork, 1);
          updateNetworkResourcesStatistic();
//          updateNetworkResources();
        });
      };

      $scope.deleteStorage = function(id) {
        cloudServices.removeStorage({
          id: $scope.cloud.id,
          storageId: id
        }, undefined, function(success) {
          delete $scope.storages[id];
          UTILS.arrayRemove($scope.cloud.storages, id);
          updateStorageResourcesStatistic();
//          updateNetworkResources();
        });
      };
      
//      $scope.saveNetworkResource = function(network) {
//        if (network.paaSResourceId === null || network.paaSResourceId === '') {
//          delete network.paaSResourceId;
//        }
//        cloudServices.setNetworkResource({
//          id: $scope.cloud.id,
//          networkName: network.networkName,
//          resourceId: network.paaSResourceId
//        }, undefined, function() {
//          updateNetworkResourcesStatistic();
//        });
//      };

      $scope.selectTemplate = function(template) {
        $scope.selectedTemplate = template;
      };

      $scope.toggleEnableTemplate = function(template) {
        cloudServices.setCloudTemplateStatus({
          id: $scope.cloud.id,
          imageId: template.cloudImageId,
          flavorId: template.cloudImageFlavorId,
          enabled: !template.enabled
        }, undefined, function() {
          template.enabled = !template.enabled;
          updateComputeResourcesStatistic();
        });
      };

      function onResize(width, height) {
        $scope.heightInfo = {
          height: height
        };
        $scope.$apply();
      }

      resizeServices.register(onResize, 0, 0);
      $scope.heightInfo = {
        height: resizeServices.getHeight(0)
      };

      $scope.deleteTemplateSelection = function() {
        delete $scope.selectedTemplate;
      };

      $scope.saveComputeTemplateResource = function(template) {
        if (template.paaSResourceId === null || template.paaSResourceId === '') {
          delete template.paaSResourceId;
        }
        cloudServices.setCloudTemplateResource({
          id: $scope.cloud.id,
          imageId: template.cloudImageId,
          flavorId: template.cloudImageFlavorId,
          resourceId: template.paaSResourceId
        }, undefined, function() {
          updateComputeResourcesStatistic();
        });
      };
      
      // id of images candidat to be added
      $scope.imageAddSelection = [];
      
      $scope.removeCloudImage = function(imageId) {
          cloudServices.removeImage({
              id: $scope.cloud.id, imageId: imageId
            }, undefined, function(success) {
              UTILS.arrayRemove($scope.cloud.images, imageId);
              delete $scope.images[imageId];
              updateImageResourcesStatistic();
              updateComputeResources(success.data);
              $scope.initSearchImageService();
          });
      }
      
      $scope.imageQueryProvider = {
        query: '',
        onSearchCompleted: function(searchResult) {
          $scope.searchImageData = searchResult.data;
//          angular.forEach($scope.searchImageData.data, function(value, key) {
//            // we store the result images in a global map
////            $scope.images[value.id] = {resource: value};
//          });
        }
      }
      $scope.initSearchImageService = function() {
        $scope.searchImageService = searchServiceFactory('rest/cloud-images/search', false, $scope.imageQueryProvider, 5, undefined, undefined, undefined, {
          exclude: $scope.cloud.images
        });
        $scope.searchImage();      
      };
      $scope.searchImage = function() {
        $scope.imageAddSelection = [];
        $scope.searchImageService.search();
      };      
      $scope.imageQueryChanged = function(query) {
        $scope.imageQueryProvider.query = query;
      };      
      
      $scope.switchCloudImageAddSelection = function(imageId) {
        if (UTILS.arrayContains($scope.imageAddSelection, imageId)) {
          UTILS.arrayRemove($scope.imageAddSelection, imageId);
        } else {
          $scope.imageAddSelection.push(imageId);
        }
      }      
      $scope.isInCloudImageAddSelection = function(imageId) {
        return UTILS.arrayContains($scope.imageAddSelection, imageId);
      }
      $scope.performAddCloudImageSelection = function() {
        cloudServices.addImage({
          id: $scope.cloud.id
        }, angular.toJson($scope.imageAddSelection), function(success) {
          // this is only ids
          $scope.cloud.images = UTILS.concat($scope.cloud.images, $scope.imageAddSelection);
          // add to the images details map
          angular.forEach($scope.searchImageData.data, function(value, key) {
              if (UTILS.arrayContains($scope.imageAddSelection, value.id)) {
                $scope.images[value.id] = {resource: value};
              }
          });          
//          updateComputeResources(success.data);
          $scope.imageAddSelection = [];
          updateImageResourcesStatistic();
          $scope.initSearchImageService();
        });          
      }      
      $scope.createCloudImage = function() {
        var modalInstance = $modal.open({
          templateUrl: 'views/cloud-images/new_cloud_image.html',
          controller: 'NewCloudImageController',
          windowClass: 'newImageModal'
        });

        modalInstance.result.then(function(cloudImageId) {
          
          cloudImageServices.get({id : cloudImageId}, function(success) {
            $scope.images[success.data.id] = {resource : success.data};
            cloudServices.addImage({
              id: $scope.cloud.id
            }, angular.toJson([cloudImageId]), function(success) {
              $scope.cloud.images = UTILS.concat($scope.cloud.images, [cloudImageId]);
              updateImageResourcesStatistic();
              updateComputeResources(success.data);
            });             
          });
        });        
      }
      
      // associate a PaaS resource id to a cloud image 
      $scope.saveImageResourceId = function(cloudImageId, paaSResourceId) {
        savePasSResourceId(
            cloudImageId, 
            $scope.images, 
            paaSResourceId, 
            cloudServices.setCloudImageResource, 
            updateImageResourcesStatistic);        
      };    
      
      // associate a PaaS resource id to a cloud flavor 
      $scope.saveFlavorResourceId = function(cloudFlavorId, paaSResourceId) {
        savePasSResourceId(
            cloudFlavorId, 
            $scope.flavors, 
            paaSResourceId, 
            cloudServices.setCloudFlavorResource, 
            updateFlavorResourcesStatistic);
      };
      
      // associate a PaaS resource id to a cloud network 
      $scope.saveNetworkResourceId = function(cloudNetworkId, paaSResourceId) {
        savePasSResourceId(
            cloudNetworkId, 
            $scope.networks, 
            paaSResourceId, 
            cloudServices.setCloudNetworkResource, 
            updateNetworkResourcesStatistic);
      };   
      
      // associate a PaaS resource id to a cloud storage 
      $scope.saveStorageResourceId = function(cloudStorageId, paaSResourceId) {
        savePasSResourceId(
            cloudStorageId, 
            $scope.storages, 
            paaSResourceId, 
            cloudServices.setCloudStorageResource, 
            updateStorageResourcesStatistic);
      };        
      
      // a generic fn that associate an internal resource to a PaaS resource
      var savePasSResourceId = function(alienResourceId, alienResourceArray, paaSResourceId, saveFn, callbackFn) {
        if (paaSResourceId === null || paaSResourceId === '') {
          delete alienResourceArray[alienResourceId].paaSResourceId;
        } else {
          alienResourceArray[alienResourceId].paaSResourceId = paaSResourceId;
        }
        saveFn({
          id: $scope.cloud.id,
          resourceId: alienResourceId,
          pasSResourceId: paaSResourceId
        }, undefined, function() {
          callbackFn();
        });        
      };
      
    }
  ]);
