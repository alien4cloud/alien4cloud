// RuntimeEventService is responsible for registration to deployment events for the runtime view.
// It also process events to enrich them with various display data.
define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-applications').factory('a4cRuntimeEventService', ['deploymentServices', 'applicationEventServicesFactory',
    function(deploymentServices, applicationEventServicesFactory) {
      var eventTypeLabels = {
        'ALL': 'APPLICATIONS.RUNTIME.EVENTS.ALL',
        'paasdeploymentstatusmonitorevent': 'APPLICATIONS.RUNTIME.EVENTS.STATUS',
        'paasinstancestatemonitorevent': 'APPLICATIONS.RUNTIME.EVENTS.INSTANCES',
        'paasinstancepersistentresourcemonitorevent': 'APPLICATIONS.RUNTIME.EVENTS.STORAGE',
        'paasmessagemonitorevent': 'APPLICATIONS.RUNTIME.EVENTS.MESSAGES'
      };

      var RuntimeEventService = function(scope, applicationId, pageStateId) {
        this.scope = scope;
        this.scope.eventTypeLabels = eventTypeLabels;
        this.applicationId = applicationId;
        this.pageStateId = pageStateId;
        this.applicationEventServices = null;

        var _this = this;
        // ensure that we remove registrations on scope destruction.
        scope.$on('$destroy', function() {
          if (_this.applicationEventServices !== null) {
            _this.applicationEventServices.unsubscribe(pageStateId);
            _this.applicationEventServices.stop();
          }
        });
        // Ensure that we update registration in case of an environment change.
        scope.$on('a4cRuntimeTopologyLoaded', function() {
          _this.register();
          _this.refreshPAASEvents();
        });
      };

      RuntimeEventService.prototype = {
        constructor: RuntimeEventService,

        register: function() {
          var _this = this;
          // if we already have a listener then stop it
          if (this.applicationEventServices !== null) {
            this.applicationEventServices.stop();
          }
          this.applicationEventServices = applicationEventServicesFactory(this.applicationId, this.scope.environment.id);
          this.applicationEventServices.start();
          this.applicationEventServices.subscribe(this.pageStateId, function(type, event) {
            _this.onPAASEvent(type, event);
          });
        },

        enrichPAASEvent: function(event) {
          event.type = eventTypeLabels[event.rawType];
          switch (event.rawType) {
            case 'paasdeploymentstatusmonitorevent':
              event.message = {
                template: 'APPLICATIONS.RUNTIME.EVENTS.DEPLOYMENT_STATUS_MESSAGE',
                data: {
                  status: 'DEPLOYMENT.STATUS.' + event.deploymentStatus
                }
              };
              break;
            case 'paasinstancestatemonitorevent':
              if (_.defined(event.instanceState)) {
                event.message = {
                  template: 'APPLICATIONS.RUNTIME.EVENTS.INSTANCE_STATE_MESSAGE',
                  data: {
                    state: event.instanceState,
                    nodeId: event.nodeTemplateId,
                    instanceId: event.instanceId
                  }
                };
              } else {
                event.message = {
                  template: 'APPLICATIONS.RUNTIME.EVENTS.INSTANCE_DELETED_MESSAGE',
                  data: {
                    nodeId: event.nodeTemplateId,
                    instanceId: event.instanceId
                  }
                };
              }
              break;
            case 'paasinstancepersistentresourcemonitorevent':
              event.message = {
                template: 'APPLICATIONS.RUNTIME.EVENTS.STORAGE_MESSAGE',
                data: {
                  nodeId: event.nodeTemplateId,
                  instanceId: event.instanceId,
                  volumeId: _.get(event.persistentProperties, 'volume_id'),
                  zone: _.get(event.persistentProperties, 'zone')
                }
              };
              break;
            case 'paasmessagemonitorevent':
              event.message = {
                template: event.message
              };
              break;
          }
        },

        refreshPAASEvents: function() {
          var _this = this;
          deploymentServices.getEvents({
            applicationEnvironmentId: this.scope.environment.id
          }, function(result) {
            // display events
            if (_.undefined(result.data) || _.undefined(result.data.data)) {
              _this.scope.events = {};
              _this.scope.events.data = [];
            } else {
              for (var i = 0; i < result.data.data.length; i++) {
                var event = result.data.data[i];
                event.rawType = result.data.types[i];
                _this.enrichPAASEvent(event);
              }
              _this.scope.events = result.data;
            }
          });
        },

        /**
        * Callback triggered when an event is received.
        */
        onPAASEvent: function(type, event) {
          console.log("----- Received an event of type " + type + " - " + event.eventType)
          event.rawType = type;
          this.enrichPAASEvent(event);
          this.scope.events.data.push(event);
          this.scope.$broadcast('a4cRuntimeEventReceived', event);
        }
      };

      return function(scope, applicationId, pageStateId) {
        return new RuntimeEventService(scope, applicationId, pageStateId);
      };
    }
  ]);
});
