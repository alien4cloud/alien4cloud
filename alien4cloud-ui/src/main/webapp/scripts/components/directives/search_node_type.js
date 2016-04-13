define(function (require) {
  'use strict';

  var modules = require('modules');
  require('scripts/components/controllers/component_search');

  modules.get('a4c-components').directive('alienSearchNodeType', ['$interval', function($interval) {
    return {
      templateUrl: 'views/components/search_node_type_template.html',
      restrict: 'E',
      scope: {
        'refresh': '=',
        'displayDetailPannel': '=',
        'onSelectItem': '&',
        'globalContext': '=',
        'dragAndDropEnabled': '=',
        'heightInfo': '=',
        'widthInfo': '=',
        'defaultFilters': '='
      },
      link: function(scope, element) {
        scope.queryComponentType = 'NODE_TYPE';

        function resize() {
          var listHeight = scope.heightInfo.height - element.offset().top - 74;
          scope.listHeight = scope.globalContext ? 'height: ' + listHeight + 'px' : '';
        }
        scope.$watch('heightInfo', function() {
          resize();
        });
        scope.$watch('widthInfo', function() {
          resize();
        });

        $interval(resize, 10, 1);
      }
    };
  }]);
});
