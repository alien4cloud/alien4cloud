/* global define, hopscotch */

'use strict';

define(function(require) {
  var modules = require('modules');
  require('toaster');
  var _ = require('lodash');
  var angular = require('angular');
  require('hopscotch');

  modules.get('ng-hopscotch', ['toaster']).factory('hopscotchService', ['$http', '$translate', '$state', '$interval', 'toaster',
    function($http, $translate, $state, $interval, toaster) {
      return {
        currentTour: null,
        startTour: function(tourName) {
          if (_.undefined(tourName)) {
            // tour name is generated from the current location.
            tourName = $state.current.name;
          }
          var instance = this;
          $http.get('data/guides/' + tourName + '-' + $translate.use() + '.json')
            .then(function(data) {
              instance.currentTour = data.data;
              hopscotch.endTour(true);
              var clickTarget = null;
              var autoResume = false;
              instance.currentTour.onShow = function() {
                var tour = hopscotch.getCurrTour();
                var step = tour.steps[hopscotch.getCurrStepNum()];
                if (step.multipage) {
                  autoResume = true;
                } else {
                  autoResume = false;
                }
                if (step.nextOnTargetClick) {
                  clickTarget = hopscotch.getCurrTarget();
                } else {
                  clickTarget = null;
                }
              };
              instance.currentTour.onNext = function() {
                if (clickTarget !== null) {
                  angular.element(clickTarget).trigger('click');
                }
                if (autoResume) {
                  $interval(function() {
                    instance.resumeTour(tourName + '-hopscotch', hopscotch.getCurrStepNum());
                  }, 10, 1);
                }
              };
              instance.currentTour.onEnd = function() {
                if (clickTarget !== null) {
                  angular.element(clickTarget).trigger('click');
                }
              };
              hopscotch.startTour(instance.currentTour);

            })
            .catch(function() {
              toaster.pop('info', $translate.instant('HOPSCOTCH_MISSING.TITLE'), $translate.instant('HOPSCOTCH_MISSING.CONTENT'), 2000, 'trustedHtml', null);
            });
        },
        resumeTour: function(tourName, step) {
          if (this.currentTour !== null && hopscotch.getState() === tourName + ':' + step) {
            hopscotch.startTour(this.currentTour, step);
          }
        }
      };
    }
  ]);
});
