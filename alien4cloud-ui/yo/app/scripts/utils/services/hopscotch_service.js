/* global UTILS, hopscotch */

'use strict';

angular.module('alienUiApp').factory('hopscotchService', ['$http', '$translate', '$state', '$interval', 'toaster',
  function($http, $translate, $state, $interval, toaster) {
    return {
      currentTour: null,
      startTour: function(tourName) {
        if (UTILS.isUndefinedOrNull(tourName)) {
          // tour name is generated from the current location.
          tourName = $state.current.name;
        }
        var instance = this;
        $http.get('data/guides/' + tourName + '-' + $translate.uses() + '.json')
          .success(function(data, status, headers, config) {
            instance.currentTour = data;
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
          .error(function(data, status, headers, config) {
            toaster.pop('info', $translate('HOPSCOTCH_MISSING.TITLE'), $translate('HOPSCOTCH_MISSING.CONTENT'), 2000, 'trustedHtml', null);
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
