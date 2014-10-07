'use strict';

angular.module('alienUiApp').factory('adminMenu', ['$resource', function($resource) {
  return [
    {
      id: 'am.admin.users',
      state: 'admin.users',
      key: 'NAVADMIN.MENU_USERS',
      icon: 'fa fa-users',
      show: true
    },
    {
      id: 'am.admin.plugins',
      state: 'admin.plugins',
      key: 'NAVADMIN.MENU_PLUGINS',
      icon: 'fa fa-puzzle-piece',
      show: true
    },
    {
      id: 'am.admin.clouds.list',
      state: 'admin.clouds.list',
      key: 'NAVADMIN.MENU_CLOUDS',
      icon: 'fa fa-cloud',
      show: true
    },
    {
      id: 'am.admin.cloud-images.list',
      state: 'admin.cloud-images.list',
      key: 'NAVADMIN.MENU_CLOUD_IMAGES',
      icon: 'fa fa-image',
      show: true
    },
    {
      id: 'am.admin.metaprops.list',
      state: 'admin.metaprops.list',
      key: 'NAVADMIN.MENU_TAGS',
      icon: 'fa fa-tags',
      show: true
    },
    {
      id: 'am.admin.metrics',
      state: 'admin.metrics',
      key: 'NAVADMIN.MENU_METRICS',
      icon: 'fa fa-tachometer',
      show: true
    }
  ];
}]);
