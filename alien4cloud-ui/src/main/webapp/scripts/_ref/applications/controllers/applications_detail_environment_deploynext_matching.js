define(function (require) {
  'use strict';

  var states = require('states');

  require('scripts/_ref/applications/controllers/applications_detail_environment_deploynext_matching_nodes');
  require('scripts/_ref/applications/controllers/applications_detail_environment_deploynext_matching_policies');

  states.state('applications.detail.environment.deploynext.matching', {
    url: '/matching',
    templateUrl: 'views/_ref/layout/tab_menu_layout.html',
    controller: 'LayoutCtrl',
    menu: {
      id: 'applications.detail.environment.deploynext.matching',
      state: 'applications.detail.environment.deploynext.matching',
      key: 'NAVAPPLICATIONS.MENU_DEPLOY_NEXT.MATCHING',
      icon: '',
      priority: 500,
      step: {
        taskCodes: ['NO_NODE_MATCHES', 'NODE_NOT_SUBSTITUTED', 'IMPLEMENT', 'REPLACE']
      }
    },
    //register breadcrumbs on entering the state
    onEnter: ['breadcrumbsService', '$translate', function(breadcrumbsService, $translate){
      breadcrumbsService.putConfig({
        state : 'applications.detail.environment.deploynext.matching',
        text: function(){
          return $translate.instant('NAVAPPLICATIONS.MENU_DEPLOY_NEXT.MATCHING');
        },
      });
    }],
  });

  states.forward('applications.detail.environment.deploynext.matching', 'applications.detail.environment.deploynext.matching.policies');

});
