/* global UTILS */
'use strict';

angular.module('alienUiApp').factory('applicationEventServicesFactory', ['deploymentEventServicesFactory', 'applicationServices',
  function(deploymentEventServicesFactory, applicationServices) {
    return function(applicationId, selectedEnvironmentId) {
      var deploymentEventServices = null;
      var applicationEventServices = {};
      var subscribeQueue = [];

      applicationEventServices.start = function() {
        applicationEventServices.doStart();
      };

      applicationEventServices.doStart = function(existingListeners) {
        applicationServices.getActiveDeployment.get({
          applicationId: applicationId,
          applicationEnvironmentId: selectedEnvironmentId
        }, undefined, function(success) {
          if (UTILS.isDefinedAndNotNull(success.data)) {
            deploymentEventServices = deploymentEventServicesFactory(success.data.id, existingListeners);
            for (var i = 0; i < subscribeQueue.length; i++) {
              doSubscribe(subscribeQueue[i].listenerId, subscribeQueue[i].type, subscribeQueue[i].callback);
            }
            subscribeQueue = [];
          }
        });
      };

      applicationEventServices.stop = function() {
        if (UTILS.isDefinedAndNotNull(deploymentEventServices)) {
          deploymentEventServices.close();
          deploymentEventServices = null;
        }
      };

      applicationEventServices.restart = function() {
        var existingListeners;
        if (UTILS.isDefinedAndNotNull(deploymentEventServices)) {
          existingListeners = deploymentEventServices.allListeners;
          deploymentEventServices.close();
          deploymentEventServices = null;
        }
        applicationEventServices.doStart(existingListeners);
      };

      applicationEventServices.subscribe = function(listenerId, callback) {
        applicationEventServices.subscribeToStatusChange(listenerId, callback);
        applicationEventServices.subscribeToInstanceStateChange(listenerId, callback);
        applicationEventServices.subscribeToStorageInstanceStateChange(listenerId, callback);
        applicationEventServices.subscribeToPaasMessage(listenerId, callback);
      };

      var doSubscribe = function(listenerId, type, callback) {
        if (UTILS.isDefinedAndNotNull(deploymentEventServices)) {
          deploymentEventServices.subscribe(listenerId, type, callback);
        } else {
          subscribeQueue.push({
            listenerId: listenerId,
            type: type,
            callback: callback
          });
        }
      };

      applicationEventServices.subscribeToStatusChange = function(listenerId, callback) {
        doSubscribe(listenerId, 'paasdeploymentstatusmonitorevent', callback);
      };

      applicationEventServices.subscribeToInstanceStateChange = function(listenerId, callback) {
        doSubscribe(listenerId, 'paasinstancestatemonitorevent', callback);
      };
      applicationEventServices.subscribeToStorageInstanceStateChange = function(listenerId, callback) {
        doSubscribe(listenerId, 'paasinstancestoragemonitorevent', callback);
      };

      applicationEventServices.subscribeToPaasMessage = function(listenerId, callback) {
        doSubscribe(listenerId, 'paasmessagemonitorevent', callback);
      };

      applicationEventServices.unsubscribe = function(listenerId) {
        applicationEventServices.unsubscribeToStatusChange(listenerId);
        applicationEventServices.unsubscribeToInstanceStateChange(listenerId);
        applicationEventServices.unsubscribeToStorageInstanceStateChange(listenerId);
        applicationEventServices.unsubscribeToPaasMessage(listenerId);
      };

      applicationEventServices.unsubscribeToStatusChange = function(listenerId) {
        if (UTILS.isDefinedAndNotNull(deploymentEventServices)) {
          deploymentEventServices.unsubscribe(listenerId, 'paasdeploymentstatusmonitorevent');
        }
      };

      applicationEventServices.unsubscribeToStorageInstanceStateChange = function(listenerId) {
        if (UTILS.isDefinedAndNotNull(deploymentEventServices)) {
          deploymentEventServices.unsubscribe(listenerId, 'paasinstancestoragemonitorevent');
        }
      };

      applicationEventServices.unsubscribeToInstanceStateChange = function(listenerId) {
        if (UTILS.isDefinedAndNotNull(deploymentEventServices)) {
          deploymentEventServices.unsubscribe(listenerId, 'paasinstancestatemonitorevent');
        }
      };

      applicationEventServices.unsubscribeToPaasMessage = function(listenerId) {
        if (UTILS.isDefinedAndNotNull(deploymentEventServices)) {
          deploymentEventServices.unsubscribe(listenerId, 'paasmessagemonitorevent');
        }
      };

      return applicationEventServices;
    };
  }
]);
