/* global UTILS */

'use strict';

var alien4cloudApp = angular.module('alienUiApp', ['ngCookies', 'ngResource', 'ngSanitize', 'ui.router', 'alienAuth', 'searchServices',
    'pascalprecht.translate', 'ui.bootstrap', 'angularFileUpload', 'xeditable', 'ui.ace', 'toaster', 'angular-utils-ui', 'treeControl'
  ])
  .config(function($stateProvider, $urlRouterProvider, $httpProvider, $parseProvider) {
    $parseProvider.unwrapPromises(true);
    $httpProvider.interceptors.push('restTechnicalErrorInterceptor');

    $urlRouterProvider.when('/applications', '/applications/list');
    $urlRouterProvider.when('/topologytemplates', '/topologytemplates/list');
    $urlRouterProvider.when('/components', '/components/list');
    $urlRouterProvider.otherwise('/');

    // state for restricted access
    $stateProvider.state('restricted', {
      url: '/restricted',
      templateUrl: 'views/restricted.html'
    })

    .state('home', {
      url: '/',
      templateUrl: 'views/main.html'
    })

    .state('home_user', {
      templateUrl: 'views/main.html'
    })

    .state('user_home_admin', {
      templateUrl: 'views/admin/home.html',
      controller: 'AdminHomeCtrl'
    })

    // components states
    .state('components', {
      url: '/components',
      templateUrl: 'views/common/vertical_menu_layout.html',
      controller: 'ComponentCtrl'
    }).state('components.list', {
      url: '/list',
      templateUrl: 'views/components/component_list.html',
      controller: 'SearchComponentCtrl'
    }).state('components.detail', {
      url: '/component/:id',
      templateUrl: 'views/components/component_details.html',
      controller: 'ComponentDetailsCtrl'
    }).state('components.csars', {
      url: '/csars',
      template: '<ui-view/>'
    }).state('components.csars.list', {
      url: '/list',
      templateUrl: 'views/components/csar_list.html',
      controller: 'CsarListCtrl'
    }).state('components.csars.csardetail', {
      url: '/:csarId',
      templateUrl: 'views/components/csar_details.html',
      controller: 'CsarDetailsCtrl'
    }).state('components.csars.csardetailnode', {
      url: '/:csarId/node/:nodeTypeId',
      templateUrl: 'views/csar_components/component_details.html',
      controller: 'CsarComponentDetailsCtrl'
    })

    // applications states
    .state('applications', {
        url: '/applications',
        template: '<ui-view/>'
      }).state('applications.list', {
        url: '/list',
        templateUrl: 'views/applications/application_list.html',
        controller: 'ApplicationListCtrl'
      }).state('applications.detail', {
        url: '/:id',
        resolve: {
          application: ['applicationServices', '$stateParams',
            function(applicationServices, $stateParams) {
              return applicationServices.get({
                applicationId: $stateParams.id
              }).$promise;
            }
          ],
          appEnvironments: ['application', 'applicationEnvironmentServices',
            function(application, applicationEnvironmentServices) {
              return applicationEnvironmentServices.getAllEnvironments(application.data.id).then(function(result) {
                return {
                  environments: result.data.data
                };
              });
            }
          ],
          appVersions: ['$http', 'application', 'applicationVersionServices',
            function($http, application, applicationVersionServices) {
              var searchAppVersionRequestObject = {
                'from': 0,
                'size': 20
              };
              return applicationVersionServices.searchVersion({
                applicationId: application.data.id
              }, angular.toJson(searchAppVersionRequestObject)).$promise.then(function(result) {
                return result.data.data;
              });
            }
          ]
        },
        templateUrl: 'views/applications/vertical_menu_layout.html',
        controller: 'ApplicationCtrl'
      }).state('applications.detail.info', {
        url: '/infos',
        templateUrl: 'views/applications/application_infos.html',
        controller: 'ApplicationInfosCtrl',
        resolve: {
          defaultEnvironmentTab: function(appEnvironments) {
            // return the first deployed env found or null
            var onlyDeployed = appEnvironments.environments.filter(function deployed(element) {
              return element.status === 'DEPLOYED';
            });
            return onlyDeployed.length > 0 ? onlyDeployed[0] : null;
          }
        }
      }).state('applications.detail.topology', {
        url: '/topology',
        templateUrl: 'views/topology/topology_editor.html',
        controller: 'TopologyCtrl',
        resolve: {
          topologyId: function() {
            return null;
          }
        }
      })
      .state('applications.detail.deployment', {
        url: '/deployment',
        templateUrl: 'views/applications/application_deployment.html',
        controller: 'ApplicationDeploymentCtrl'
      }).state('applications.detail.runtime', {
        url: '/runtime',
        templateUrl: 'views/applications/topology_runtime.html',
        controller: 'TopologyRuntimeCtrl'
      }).state('applications.detail.users', {
        url: '/users',
        templateUrl: 'views/applications/application_users.html',
        resolve: {
          applicationRoles: ['$resource',
            function($resource) {
              return $resource('rest/auth/roles/application', {}, {
                method: 'GET'
              }).get().$promise;
            }
          ],
          environmentRoles: ['$resource',
            function($resource) {
              return $resource('rest/auth/roles/environment', {}, {
                method: 'GET'
              }).get().$promise;
            }
          ]
        },
        controller: 'ApplicationUsersCtrl'
      }).state('applications.detail.environments', {
        url: '/environment',
        templateUrl: 'views/applications/application_environments.html',
        controller: 'ApplicationEnvironmentsCtrl'
      }).state('applications.detail.versions', {
        url: '/versions',
        templateUrl: 'views/applications/application_versions.html',
        controller: 'ApplicationVersionsCtrl'
      })

      // topology templates
      .state('topologytemplates', {
        url: '/topologytemplates',
        template: '<ui-view/>'
      }).state('topologytemplates.list', {
        url: '/list',
        templateUrl: 'views/topologytemplates/topology_template_list.html',
        controller: 'TopologyTemplateListCtrl'
      }).state('topologytemplates.detail', {
        url: '/:id',
        templateUrl: 'views/topologytemplates/topology_template.html',
        resolve: {
          topologyTemplate: ['topologyTemplateService', '$stateParams',
            function(topologyTemplateService, $stateParams) {
              return topologyTemplateService.get({
                topologyTemplateId: $stateParams.id
              }).$promise;
            }
          ]
        },
        controller: 'TopologyTemplateCtrl'
      }).state('topologytemplates.detail.topology', {
        url: '/topology',
        templateUrl: 'views/topology/topology_editor.html',
        resolve: {
          topologyId: ['topologyTemplate',
            function(topologyTemplate) {
              return topologyTemplate.data.topologyId;
            }
          ],
          appVersions: function() {
            // TODO : handle versions for topology templates
            return null;
          }
        },
        controller: 'TopologyCtrl'
      })

      // administration
      .state('admin', {
        url: '/admin',
        templateUrl: 'views/common/vertical_menu_layout.html',
        controller: 'AdminCtrl'
      }).state('admin.home', {
        url: '/',
        templateUrl: 'views/common/vertical_menu_homepage_layout.html',
        controller: 'AdminLayoutHomeCtrl'
      }).state('admin.users', {
        url: '/users',
        templateUrl: 'views/users/user_list.html',
        controller: 'UsersCtrl'
      }).state('admin.plugins', {
        url: '/plugins',
        templateUrl: 'views/plugin_list.html',
        controller: 'PluginCtrl'
      }).state('admin.metaprops', {
        url: '/metaproperties',
        template: '<ui-view/>'
      }).state('admin.metaprops.list', {
        url: '/list',
        templateUrl: 'views/meta-props/tag_configuration_list.html',
        controller: 'TagConfigurationListCtrl'
      }).state('admin.metaprops.detail', {
        url: '/:id',
        templateUrl: 'views/meta-props/tag_configuration.html',
        controller: 'TagConfigurationCtrl'
      }).state('admin.clouds', {
        url: '/clouds',
        template: '<ui-view/>'
      }).state('admin.clouds.list', {
        url: '/list',
        templateUrl: 'views/clouds/cloud_list.html',
        controller: 'CloudListController'
      }).state('admin.clouds.detail', {
        url: '/:id',
        templateUrl: 'views/clouds/cloud_detail.html',
        controller: 'CloudDetailController'
      }).state('admin.metrics', {
        url: '/metrics',
        templateUrl: 'views/admin/metrics.html',
        controller: 'MetricsController'
      }).state('admin.audit', {
        url: '/audit',
        templateUrl: 'views/admin/metrics.html',
        controller: 'MetricsController'
      }).state('admin.cloud-images', {
        url: '/cloud-images',
        template: '<ui-view/>'
      }).state('admin.cloud-images.list', {
        url: '/list',
        templateUrl: 'views/cloud-images/cloud_image_list.html',
        controller: 'CloudImageListController'
      }).state('admin.cloud-images.detail', {
        url: '/:id?mode',
        resolve: {
          cloudImage: ['cloudImageServices', '$stateParams',
            function(cloudImageServices, $stateParams) {
              return cloudImageServices.get({
                id: $stateParams.id
              }).$promise.then(function(success) {
                var cloudImage = success.data;
                if (UTILS.isDefinedAndNotNull(cloudImage.requirement)) {
                  cloudImage.numCPUs = cloudImage.requirement.numCPUs;
                  cloudImage.diskSize = cloudImage.requirement.diskSize;
                  cloudImage.memSize = cloudImage.requirement.memSize;
                }
                return cloudImage;
              });
            }
          ]
        },
        templateUrl: 'views/cloud-images/cloud_image_detail.html',
        controller: 'CloudImageDetailController'
      });
    });

/* i18n : angular-translate configuration */
alien4cloudApp.config(['$translateProvider',
  function($translateProvider) {
    $translateProvider.translations({
      CODE: 'fr-fr'
    });
    // Default language to load
    $translateProvider.preferredLanguage('fr-fr');

    // Static file loader
    $translateProvider.useStaticFilesLoader({
      prefix: 'data/languages/locale-',
      suffix: '.json'
    });
  }
]);

alien4cloudApp.run(['alienNavBarService', 'editableOptions', 'editableThemes', 'quickSearchServices', '$rootScope', 'alienAuthService', '$state',
  function(alienNavBarService, editableOptions, editableThemes, quickSearchServices, $rootScope, alienAuthService, $state) {
    alienNavBarService.menu.navbrandimg = 'images/cloudalien-small.png';

    // check when the state is about to change
    $rootScope.$on('$stateChangeStart', function(event, toState) {
      alienAuthService.getStatus().$promise.then(function() {
        if (toState.name.indexOf('home') === 0 && alienAuthService.hasRole('ADMIN')) {
          $state.go('user_home_admin');
        }
        // check all the menu array & permissions
        alienNavBarService.menu.left.forEach(function(menuItem) {
          var menuType = menuItem.id.split('.')[1];
          var foundMenuIndex = toState.name.indexOf(menuType);
          if (foundMenuIndex === 0 && menuItem.hasRole === false) {
            $state.go('restricted');
          }
        });
      });
    });

    var stateForward = {
      applications: 'applications.list',
      topologytemplates: 'topologytemplates.list',
      components: 'components.list',
      admin: 'admin.home'
    };

    // state routing
    $rootScope.$on('$stateChangeSuccess', function(event, toState) {
      var forward = stateForward[toState.name];
      if(UTILS.isDefinedAndNotNull(forward)) {
        $state.go(forward);
      }
    });

    alienNavBarService.menu.left = [{
      'roles': [],
      'id': 'menu.applications',
      'key': 'NAVBAR.MENU_APPS',
      'state': 'applications',
      'icon': 'fa fa-desktop'
    }, {
      'roles': ['ARCHITECT'],
      'id': 'menu.topologytemplates',
      'key': 'NAVBAR.MENU_TOPOLOGY_TEMPLATE',
      'state': 'topologytemplates',
      'icon': 'fa fa-sitemap'
    }, {
      'roles': ['COMPONENTS_MANAGER', 'COMPONENTS_BROWSER'],
      'id': 'menu.components',
      'key': 'NAVBAR.MENU_COMPONENTS',
      'state': 'components',
      'icon': 'fa fa-cubes'
    }, {
      'roles': ['ADMIN'],
      'id': 'menu.admin',
      'key': 'NAVBAR.MENU_ADMIN',
      'state': 'admin',
      'icon': 'fa fa-wrench'
    }];

    alienNavBarService.quickSearchHandler = {
      'doQuickSearch': quickSearchServices.doQuickSearch,
      'onItemSelected': quickSearchServices.onItemSelected,
      'waitBeforeRequest': 500,
      'minLength': 3
    };

    /* angular-xeditable config */
    editableThemes.bs3.inputClass = 'input-sm';
    editableThemes.bs3.buttonsClass = 'btn-sm';
    editableThemes.bs3.submitTpl = '<button type="button" class="btn btn-primary"' +
      ' confirm="{{\'CONFIRM_MESSAGE\' | translate}}"' +
      ' confirm-title="{{\'CONFIRM\' | translate }}"' +
      ' confirm-placement="left"' +
      ' cancel-handler="$form.$cancel()"' +
      ' ng-click="$event.stopPropagation();">' +
      '<span class="fa fa-check"></span>' +
      '</button>';
    editableThemes.bs3.cancelTpl = '<button type="button" class="btn btn-default" ng-click="$form.$cancel()">' +
      '<span class="fa fa-times"></span>' +
      '</button>';
    editableOptions.theme = 'bs3';
  }
]);
