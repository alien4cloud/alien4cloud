'use strict';

angular.module('alienUiApp').factory('hopscotchService', ['$http', '$translate',
  function($http, $translate) {

    // configuration object
    var defaultOptions = {
      zindex: 1000
    };

    return {
      currentTour: null,
      startTour: function(tourName) {
        var instance = this;
        $http.get('data/guides/' + tourName + '-' + $translate.uses() + '.json').then(function(tour) {
          instance.currentTour = tour.data;
          instance.currentTour.onNext = function() {
            console.log('hopscotch on next target', hopscotch.getCurrTarget());
          }
          instance.currentTour.onPrev = function() {
            console.log('hopscotch on next target', hopscotch.getCurrTarget());
          }
          instance.currentTour.onEnd = function() {
            console.log('hopscotch tour ended', hopscotch.getCurrTarget());
          }
          hopscotch.endTour(true);
          hopscotch.configure(defaultOptions); // see above
          hopscotch.startTour(instance.currentTour);
        });
      },
      resumeTour: function(expectedStep, step) {
        if (this.currentTour !== null && hopscotch.getState() === expectedStep + ':' + step) {
          hopscotch.startTour(this.currentTour, step);
        }
      }
    };
  }
]);
