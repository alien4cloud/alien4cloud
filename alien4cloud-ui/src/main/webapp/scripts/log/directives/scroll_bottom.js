define(function (require) {
 'use strict';
 var modules = require('modules');
 var $ = require('jquery');

 modules.get('alien4cloud-premium-logs').directive('scrollBottom',
     function () {
       return {
         restrict: 'A',
         scope: {
           toWatch: '=scrollBottom',
           enabled: '<scrollBottomEnabled'
         },
         link: function (scope, element) {
           var container = $(element);
           var lockScroll = false;  // to lock auto scroll to bottom if the user manually scrolls
           scope.$watchCollection('toWatch', function () {
             if(!scope.enabled || lockScroll) {
               return;
             }
            //  console.log('scrolling to bottom');
             container.animate({ scrollTop: container.prop('scrollHeight')}, 1);
           });

           scope.$watch('enabled', function(newValue, oldValue){
             // unlock auto scroll when enabled changes to true
             if(newValue !== oldValue && newValue){
               lockScroll=false;
             }
           });

           /**
           ** Handle scrool locking
           **/
           var  lockScrollHandler = function() {
             if(scope.enabled) {
               // if the user manually scrolls up while being in auto scrollBottom mode, then lock the scroll
               if(container.scrollTop() + container.innerHeight() < container.prop('scrollHeight')) {
                //  console.log('Locking auto scroll');
                 lockScroll = true;
               }else{
                 //if he scrolls down to the bottom again, then re-enable auto scroll to bottom
                //  console.log('Unlocking auto scroll');
                 lockScroll = false;
               }
             }
           };

           container.on('scroll', scope.$apply.bind(scope, lockScrollHandler));
         }
       };
     }
 );

});
