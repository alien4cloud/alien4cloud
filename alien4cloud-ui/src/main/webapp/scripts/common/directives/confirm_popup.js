define(function (require) {
  'use strict';

  var modules = require('modules');

  var a4cCommonModule = modules.get('a4c-common', ['ui.bootstrap']);

  /* HTML tooltip */
  var htmlTooltipConfirm = '<div class="popover {{placement}}" ng-class="{ in: isOpen(), fade: animation() }" data-toggle="popover">' +
  '<div class="arrow"></div>' +
  '<div class="popover-inner">' +
  '<h3 class="popover-title" ng-bind="title" ng-show="title"></h3>' +
  '<div class="popover-content" ng-bind="content"></div>' +
  '<div class="popover-content">' +
  '<div class="row" style="width: 250px;">' +
  '<div class="col-xs-6">' +
  '<button class="btn-block btn btn-success" ng-click="$parent.confirm();$event.stopPropagation();">{{\'COMMON.YES\' | translate}}</button>' +
  '</div>' +
  '<div class="col-xs-6">' +
  '<button class="btn-block btn btn-danger" ng-click="$parent.cancel();$event.stopPropagation();">{{\'COMMON.NO\' | translate}}</button>' +
  '</div>' +
  '</div>' +
  '</div>' +
  '</div>' +
  '</div>';

  a4cCommonModule.directive('confirmPopup', function() {
    return {
      restrict: 'A',
      replace: true,
      scope: {
        title: '@',
        content: '@',
        placement: '@',
        animation: '&',
        isOpen: '&'
      },
      template: htmlTooltipConfirm
    };
  });

  a4cCommonModule.directive('confirm', ['$tooltip',
    function($tooltip) {
      var tt = $tooltip('confirm', 'confirm', 'click');
      tt.controller = 'ConfirmCtrl';
      return tt;
    }
  ]);

  a4cCommonModule.controller('ConfirmCtrl', ['$scope', '$attrs', '$parse', '$element', '$document',
    function($scope, $attrs, $parse, $element, $document) {
      var fn = $parse($attrs.confirmHandler);
      var cancelFn = $parse($attrs.cancelHandler);

      $scope.cancel = function() {
        cancelFn($scope);
        $scope.close();
      };

      // execute the function
      $scope.confirm = function() {
        fn($scope);
        $scope.close();
      };

      // 3 :  run close() when the click is outside the popover
      var onClick = function(event) {
        var popoverElement = $('.popover');
        var isChild = popoverElement.has(event.target).length > 0;
        var isSelf = popoverElement[0] === event.target;
        var isInside = isChild || isSelf;
        if (!isInside) {
          $scope.close();
        }
      };

      // 2 : handle all click outside of the popover
      var onButtonClick = function() {
        $document.bind('click', onClick);
      };

      // 1 : add 'click' action on the first button action
      $element.bind('click', onButtonClick);

      // 4 : at close action unbind all actions
      $scope.close = function() {
        $element.unbind('click', onButtonClick);
        $document.unbind('click', onClick);
        // mandatory for $tooltip() $aply handler
        setTimeout(function() {
          $element.click();
          $element.bind('click', onButtonClick);
        }, 0);
      };
    }
  ]);
});
