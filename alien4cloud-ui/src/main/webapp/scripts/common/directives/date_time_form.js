define(function (require) {
  'use strict';

  var modules = require('modules');
  var moment = require('moment');
  // Bootstrap.js create conflict and so we must close the dropdown our-self
  var $ = require('jquery');
  require('angular-bootstrap-datetimepicker');

  modules.get('a4c-common', ['ui.bootstrap.datetimepicker']).directive('dateTimeForm', function () {
    return {
      restrict: 'E',
      templateUrl: 'views/common/date_time_form.html',
      controller: 'DateTimeFormController',
      scope: {
        fieldName: '@',
        onDateTimeSet: '&',
        dateFormat: '@',
        position: '@'
      }
    };
  });

  modules.get('a4c-common', ['ui.bootstrap.datetimepicker']).controller('DateTimeFormController', ['$scope', '$timeout', function ($scope, $timeout) {

    $scope.dateConfig = {};

    $scope.fieldNameFormatted = $scope.fieldName.replace(/\W+/g, '');

    // When user types in the input
    $scope.dateChanged = function (value) {
      if (value === undefined || value === null || value === '') {
        delete $scope.dateConfig.date;
        delete $scope.dateConfig.dateInValid;
        delete $scope.dateConfig.dateText;
        $scope.onDateTimeSet({
          newDate: undefined
        });
        return;
      }
      var momentDate = moment(value, $scope.dateFormat, true);
      if (momentDate.isValid()) {
        $scope.dateConfig.date = moment(value, $scope.dateFormat).toDate();
        $scope.dateConfig.dateInValid = false;
        $scope.onDateTimeSet({
          newDate: $scope.dateConfig.date
        });
      } else {
        $scope.dateConfig.dateInValid = true;
      }
    };

    // When user uses date time picker to select a date time
    $scope.onSetTime = function (newDate) {
      $scope.dateConfig.dateText = moment(newDate).format($scope.dateFormat);
      $scope.dateConfig.dateInValid = false;
      $scope.onDateTimeSet({
        newDate: $scope.dateConfig.date
      });
      $timeout(function () {
        $('#' + $scope.fieldNameFormatted + 'DropDown').click();
      });
    };

  }]);

});
