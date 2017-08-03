define(function (require) {
  'use strict';

  var modules = require('modules');

  //display an info icon

  var template = '<span class="btn btn-xs info-{{size}}" ng-if = "content"'+
                  'uib-popover="{{content | translate}}"'+
                  'popover-trigger="\'{{trigger}}\'"'+
                  'popover-placement = "{{placement}}"'+
                  'popover-class="info-popover info-popover-{{size}}"'+
                  'popover-append-to-body="true">'+
                  '<i class="fa fa-info-circle text-info"></i>'+
                '</span>'  ;

  modules.get('a4c-common').directive('info', function() {
    return {
      restrict: 'E',
      template: template,
      scope: {
        content: '=',
        //  sm, lg
        size: '@?',
        trigger: '@?',
        placement: '@?'
      },
      link: function(scope) {
        scope.size = scope.size || 'xs';
        scope.trigger = scope.trigger || 'mouseenter';
        scope.placement = scope.placement || 'left';
      }
    };
  });
});
