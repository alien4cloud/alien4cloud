define(function(require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  var angular = require('angular');

  modules.get('a4c-services').directive('managedService', ['$alresource', function($alresource) {
    return {
      templateUrl: 'views/services/directives/managed_service.html',
      restrict: 'E',
      scope: {
        application: '=',
        environment: '=',
        topology: '=',
        /*true or false*/
        runtime: '=?',
        /*if needed, provide for a close function, for the vertical menu display*/
        close: '&?'
      },
      link: function (scope){

        scope._ = _;
        scope.runtime = scope.runtime || false;

        // managed service
        var managedServiceResourceService = $alresource('rest/latest/applications/:applicationId/environments/:environmentId/services');

        function getManagedService(){
          delete scope.environment.service;
          managedServiceResourceService.get({
            applicationId: scope.application.id,
            environmentId: scope.environment.id
          }, function(result){
            if(_.undefined(result.error) && _.defined(result.data)){
              scope.environment.service = result.data;
            }
            scope.serviceName = _.get(scope, 'environment.service.name') || _.capitalize(_.camelCase(scope.application.name +'_'+scope.environment.name));
          });
        }

        scope.createService = function(){
          var request = {
            serviceName: scope.serviceName,
            fromRuntime: scope.runtime
          };
          managedServiceResourceService.create({
            applicationId: scope.application.id,
            environmentId: scope.environment.id
          }, angular.toJson(request), getManagedService);
        };

        var unbind =function(){
          managedServiceResourceService.patch({
            applicationId: scope.application.id,
            environmentId: scope.environment.id
          }, null, getManagedService);
        };

        scope.tryDelete = function(){
          //first ty to delete
          managedServiceResourceService.delete({
            applicationId: scope.application.id,
            environmentId: scope.environment.id
          }, null, function(result){
            if(_.undefined(result.error)){
              getManagedService();
            }else {
              //this could mean the service is used. try to unbind it instead
              unbind();
            }
          });
        };

        scope.setName = function(name){
          scope.serviceName = name;
        };

        scope.$watch('environment.id', function(newValue) {
          if(_.undefined(newValue)){
            return;
          }
          getManagedService();
        });
      }
    };
  }]);
});
