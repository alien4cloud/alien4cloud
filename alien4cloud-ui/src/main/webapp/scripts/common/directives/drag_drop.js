'use strict';

angular.module('alienUiApp').directive('alienDraggable', ['$rootScope', function($rootScope) {
  return {
    restrict: 'A',
    scope:{
      'enabled':'=dragEnabled',
      'data':'=dragData'
    },
    link: function(scope, el) {
      if(scope.enabled) {
        angular.element(el).attr('draggable', 'true');
      }

      el.bind('dragstart', function(e) {
        e.originalEvent.dataTransfer.setData('text', angular.toJson(scope.data));
        $rootScope.$emit('DRAG-START');
      });

      el.bind('dragend', function() {
        $rootScope.$emit('DRAG-END');
      });

      //watch the bound data
      scope.$watch('enabled', function() {
        if(scope.enabled) {
          angular.element(el).attr('draggable', 'true');
        } else {
          angular.element(el).removeAttr('draggable');
        }
      });
    }
  };
}]);

angular.module('alienUiApp').directive('alienDroppable', ['$rootScope', function($rootScope) {
  return {
    restrict: 'A',
    scope: {
      data: '=dragData',
      onDrop: '&dragOnDrop'
    },
    link: function(scope, el) {
      el.bind('dragover', function(e) {
        if (e.preventDefault) {
          e.preventDefault(); // Necessary. Allows us to drop.
        }

        if(e.stopPropagation) {
          e.stopPropagation();
        }

        e.originalEvent.dataTransfer.dropEffect = 'move';
        return false;
      });

      el.bind('dragenter', function() {
        el.addClass('drag-over');
      });

      el.bind('dragleave', function() {
        el.removeClass('drag-over');
      });

      el.bind('drop', function(e) {
        if (e.preventDefault) {
          e.preventDefault(); // Necessary. Allows us to drop.
        }

        if (e.stopPropogation) {
          e.stopPropogation(); // Necessary. Allows us to drop.
        }

        var sourceData = e.originalEvent.dataTransfer.getData('text');

        scope.onDrop({data: {event: e, source: sourceData, target: scope.data}});
      });

      $rootScope.$on('DRAG-START', function() {
        el.addClass('drag-target');
      });

      $rootScope.$on('DRAG-END', function() {
        el.removeClass('drag-target');
        el.removeClass('drag-over');
      });
    }
  };
}]);
