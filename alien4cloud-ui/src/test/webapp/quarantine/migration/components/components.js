/* global by, element */
'use strict';

var navigation = require('../common/navigation');

module.exports.checkComponentManager = function(isManager) {
  var message = isManager ? 'A user that is component manager or admin should have access to the drop zone on the components list page.': 'A user that is not application manager or admin should not have access to the drop zone on the components list page.';

  browser.element(by.id('menu.components')).isPresent().then(function(present) {
    expect(present).toBe(true, message);
    if(present) {
      navigation.go('main', 'components');
      expect(browser.element(by.id('fileUpload')).isPresent()).toBe(isManager, message);
    }
  });
};

function selectComponentVersion(componentId, newVersion) {
  var versionsBtn = element(by.id(componentId + '_versions'));
  versionsBtn.click();

  browser.sleep(1000);

  var versionLink = element(by.id(componentId + '_version_' + newVersion));
  versionLink.click();
}
module.exports.selectComponentVersion = selectComponentVersion;

module.exports.goToComponentDetailPage = function(componentId) {
  navigation.go('main', 'components');

  var componentName = componentId.substring(0, componentId.indexOf(':'));
  var searchImput = element(by.model('searchedKeyword'));
  searchImput.sendKeys(componentName);
  var btnSearch = element(by.id('btn-search-component'));
  btnSearch.click();

  // From the component search page select a particular line
  var componentElement = element(by.id('li_' + componentId));
  componentElement.click();

  browser.waitForAngular();
};

module.exports.changeComponentVersionAndGo = function(componentId, newVersion) {
  navigation.go('main', 'components');

  var componentName = componentId.substring(0, componentId.indexOf(':'));
  var searchImput = element(by.model('searchedKeyword'));
  searchImput.sendKeys(componentName);
  var btnSearch = element(by.id('btn-search-component'));
  btnSearch.click();

  selectComponentVersion(componentId, newVersion);

  // From the component search page select a particular line
  var componentElement = element(by.id('li_' + componentName+':'+newVersion));
  componentElement.click();

  browser.waitForAngular();
};
