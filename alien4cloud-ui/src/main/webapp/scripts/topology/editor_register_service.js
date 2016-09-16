// Simplify the generation of crud resources for alien
define(function (require) {
  'use strict';
  var states = require('states');

  require('scripts/topology/controllers/editor');

  return function(prefix) {
    states.state(prefix + '.editor', {
      url: '/editor',
      templateUrl: 'views/topology/topology_editor.html',
      controller: 'TopologyCtrl',
      resolve: {
        defaultFilters: [function(){return {};}],
        badges: [function(){return[];}]
      },
      menu: {
        id: 'am.' + prefix + '.editor',
        state: prefix + '.editor',
        key: 'EDITOR.MENU_TOPOLOGY',
        icon: 'fa fa-sitemap',
        show: true,
        priority: 1
      }
    });

    states.state(prefix + '.workflow', {
      url: '/workflow',
      templateUrl: 'views/topology/topology_workflow_editor.html',
      controller: 'TopologyWorkflowEditorCtrl',
      menu: {
        id: 'am.' + prefix + '.workflow',
        state: prefix + '.workflow',
        key: 'EDITOR.MENU_WORKFLOW',
        icon: 'fa fa-code-fork fa-rotate-90',
        show: true,
        priority: 1
      }
    });

    states.state(prefix + '.files', {
      url: '/files',
      templateUrl: 'views/topology/topology_browser.html',
      controller: 'TopologyBrowserCtrl',
      menu: {
        id: 'am.' + prefix + '.files',
        state: prefix + '.files',
        key: 'EDITOR.MENU_FILES',
        icon: 'fa fa-folder-open',
        show: true,
        priority: 10
      }
    });

    states.state(prefix + '.validation', {
      url: '/validation',
      templateUrl: 'views/topology/topology_validation.html',
      controller: 'TopologyValidationCtrl',
      menu: {
        id: 'am.' + prefix + '.validation',
        state: prefix + '.validation',
        key: 'EDITOR.MENU_VALIDATION',
        icon: 'fa fa-check',
        show: true,
        priority: 20
      }
    });

    states.state(prefix + '.history', {
      url: '/history',
      templateUrl: 'views/topology/editor_history.html',
      controller: 'TopologyHistoryCtrl',
      menu: {
        id: 'am.' + prefix + '.history',
        state: prefix + '.history',
        key: 'EDITOR.MENU_HISTORY',
        icon: 'fa fa-history',
        show: true,
        priority: 30
      }
    });

    states.forward(prefix, prefix + '.editor');
  };
});
