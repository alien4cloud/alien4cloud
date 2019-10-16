// Simplify the generation of crud resources for alien
define(function (require) {
  'use strict';
  var states = require('states');
  var _ = require('lodash');

  require('scripts/topology/controllers/editor');

  function override(menuOverrides, name, menu) {
    var overrides = _.get(menuOverrides, name);
    if(_.defined(overrides)) {
      return _.merge(menu, overrides);
    }
    return menu;
  }

  return function(prefix, menuOverrides) {
    states.state(prefix + '.editor', {
      url: '/editor',
      templateUrl: 'views/topology/topology_editor.html',
      controller: 'TopologyCtrl',
      resolve: {
        defaultFilters: [function(){return {};}],
        badges: [function(){return[];}]
      },
      menu: override(menuOverrides, 'editor',{
        id: 'am.' + prefix + '.editor',
        state: prefix + '.editor',
        key: 'EDITOR.MENU_TOPOLOGY',
        icon: 'fa fa-sitemap',
        icon_invalid: 'fa fa-sitemap',
        invalidation_state: false,
        show: true,
        priority: 10
      })
    });

    states.state(prefix + '.workflow', {
      url: '/workflow',
      templateUrl: 'views/topology/topology_workflow_editor.html',
      controller: 'TopologyWorkflowEditorCtrl',
      menu: override(menuOverrides, 'workflow',{
        id: 'am.' + prefix + '.workflow',
        state: prefix + '.workflow',
        key: 'EDITOR.MENU_WORKFLOW',
        icon: 'fa fa-code-fork fa-rotate-90',
        icon_invalid: 'fa fa-code-fork fa-rotate-90',
        invalidation_state: false,
        show: true,
        priority: 20
      })
    });

    states.state(prefix + '.inputs', {
      url: '/inputs',
      templateUrl: 'views/topology/topology_inputs_variables.html',
      controller: 'TopologyInputsVariablesCtrl',
      menu: override(menuOverrides, 'inputs', {
        id: 'am.' + prefix + '.inputs',
        state: prefix + '.inputs',
        key: 'EDITOR.MENU_INPUTS_VAR',
        icon: 'fa fa-sign-in',
        icon_invalid: 'fa fa-sign-in',
        invalidation_state: false,
        show: true,
        priority: 25
      })
    });

    states.state(prefix + '.files', {
      url: '/files',
      templateUrl: 'views/topology/topology_browser.html',
      controller: 'TopologyBrowserCtrl',
      menu: override(menuOverrides, 'files', {
        id: 'am.' + prefix + '.files',
        state: prefix + '.files',
        key: 'EDITOR.MENU_FILES',
        icon: 'fa fa-folder-open',
        icon_invalid: 'fa fa-folder-open',
        invalidation_state: false,
        show: true,
        priority: 30
      })
    });

    states.state(prefix + '.validation', {
      url: '/validation',
      templateUrl: 'views/topology/topology_validation.html',
      controller: 'TopologyValidationCtrl',
      menu: override(menuOverrides, 'validation', {
        id: 'am.' + prefix + '.validation',
        state: prefix + '.validation',
        key: 'EDITOR.MENU_VALIDATION',
        icon: 'fa fa-check',
        icon_invalid: 'fa fa-exclamation-triangle',
        invalidation_state: true,
        show: true,
        priority: 40
      })
    });

    states.state(prefix + '.history', {
      url: '/history',
      templateUrl: 'views/topology/editor_history.html',
      controller: 'TopologyHistoryCtrl',
      menu: override(menuOverrides, 'history', {
        id: 'am.' + prefix + '.history',
        state: prefix + '.history',
        key: 'EDITOR.MENU_HISTORY',
        icon: 'fa fa-history',
        icon_invalid: 'fa fa-history',
        invalidation_state: false, // control icon validation change
        show: true,
        priority: 50
      })
    });

    states.forward(prefix, prefix + '.editor');
  };
});
