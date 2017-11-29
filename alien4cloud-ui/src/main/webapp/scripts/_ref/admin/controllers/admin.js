define(function (require) {
  'use strict';

  // create the alien4cloud-admin module for angular
  var modules = require('modules');
  var states = require('states');

  // register admin root state
  states.state('admin', {
    url: '/admin',
    templateUrl: 'views/_ref/admin/admin.html',
    controller: 'LayoutCtrl',
    menu: {
      id: 'menu.admin',
      state: 'admin',
      key: 'NAVBAR.MENU_ADMIN',
      icon: 'fa fa-wrench',
      priority: 10000,
      roles: ['ADMIN']
    }
  });
  // register admin default home page
  states.state('admin.home', {
    url: '/',
    templateUrl: 'views/_ref/admin/admin_home.html',
    controller: 'LayoutCtrl'
  });
  states.forward('admin', 'admin.home');

  var admin = modules.get('alien4cloud-admin');

  // require admin modules
  require('scripts/_ref/admin/controllers/admin_plugins');
  require('scripts/_ref/admin/controllers/admin_server');
  require('scripts/_ref/admin/controllers/admin_audit');
  require('scripts/meta-props/controllers/meta_props_list');
  require('scripts/_ref/admin/controllers/admin_repositories');

  return admin;
});
