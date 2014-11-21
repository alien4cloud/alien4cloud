/* global UTILS */
'use strict';

angular.module('alienUiApp').controller(
  'CloudDetailController', ['$scope', '$http', '$resource', '$stateParams', '$timeout', 'cloudServices', '$state', 'deploymentServices', 'toaster', '$translate', 'userServices', 'groupServices', '$modal', 'resizeServices', '$q',
    function($scope, $http, $resource, $stateParams, $timeout, cloudServices, $state, deploymentServices, toaster, $translate, userServices, groupServices, $modal, resizeServices, $q) {
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

      var updatePaaSResourceId = function() {
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

      cloudServices.get({
        id: cloudId
      }, function(response) {
        $scope.images = response.data.images;
        $scope.flavors = response.data.flavors;
        $scope.cloud = response.data.cloud;
        $scope.networks = response.data.networks;
        if (response.data.matcherConfig) {
          $scope.manualMatchResource = true;
          $scope.matchedComputeTemplates = response.data.matcherConfig.matchedComputeTemplates;
          updatePaaSResourceId();
        }
        updateTemplateStatistic();
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

      var updateTemplateStatistic = function() {
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

      var updateCloudResources = function(cloudResources) {
        var newComputeTemplates = cloudResources.computeTemplates;
        $scope.tabs.newTemplates = newComputeTemplates.length - $scope.cloud.computeTemplates.length;
        $scope.cloud.computeTemplates = newComputeTemplates;
        if (UTILS.isDefinedAndNotNull(cloudResources.matchedComputeTemplates)) {
          $scope.matchedComputeTemplates = cloudResources.matchedComputeTemplates;
        }
        updatePaaSResourceId();
        updateTemplateStatistic();
      };

      /** handle Modal form for cloud image creation */
      $scope.openFlavorCreationModal = function() {
        var modalInstance = $modal.open({
          templateUrl: 'views/clouds/new_flavor.html',
          controller: 'NewCloudImageFlavorController'
        });

        modalInstance.result.then(function(flavor) {
          cloudServices.addFlavor({
            id: $scope.cloud.id
          }, angular.toJson(flavor), function(success) {
            $scope.flavors[flavor.id] = flavor;
            $scope.cloud.flavors.push(flavor);
            updateCloudResources(success.data);
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
          updateCloudResources(success.data);
        });
      };

      $scope.openAddCloudImageModal = function() {
        var modalInstance = $modal.open({
          templateUrl: 'views/clouds/new_image.html',
          controller: 'AddCloudImageController',
          windowClass: 'clouds-add-image',
          scope: $scope
        });

        modalInstance.result.then(function(images) {
          var imageIds = [];
          for (var i = 0; i < images.length; i++) {
            imageIds.push(images[i].id);
          }
          cloudServices.addImage({
            id: $scope.cloud.id
          }, angular.toJson(imageIds), function(success) {
            for (var i = 0; i < images.length; i++) {
              $scope.images[images[i].id] = images[i];
            }
            $scope.cloud.images = UTILS.concat($scope.cloud.images, imageIds);
            updateCloudResources(success.data);
          });
        });
      };

      $scope.deleteCloudImage = function(imageId) {
        cloudServices.removeImage({
          id: $scope.cloud.id,
          imageId: imageId
        }, undefined, function(success) {
          delete $scope.images[imageId];
          var indexOfImage = $scope.cloud.images.indexOf(imageId);
          $scope.cloud.images.splice(indexOfImage, 1);
          updateCloudResources(success.data);
        });
      };

      /** handle Modal form for cloud image creation */
      $scope.openNetworkCreationModal = function() {
        var modalInstance = $modal.open({
          templateUrl: 'views/clouds/new_network.html',
          controller: 'NewNetworkController'
        });

        modalInstance.result.then(function(network) {
          cloudServices.addNetwork({
            id: $scope.cloud.id
          }, angular.toJson(network), function() {
            $scope.networks[network.id] = network;
            $scope.cloud.networks.push(network);
          });
        });
      };

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
          updateTemplateStatistic();
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
          updateTemplateStatistic();
        });
      };
    }
  ]);