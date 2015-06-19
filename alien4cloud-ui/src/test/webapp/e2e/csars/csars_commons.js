/* global by, element */
'use strict';

// TODO : update for new CSAR UI pages
var navigation = require('../common/navigation');

var goToCsarSearchPage = function() {
  navigation.go('main', 'components');
  navigation.go('components', 'csars');
};
module.exports.goToCsarSearchPage = goToCsarSearchPage;

var createCsar = function(csarName, csarVersion, description) {
  // Jump on csar list page
  goToCsarSearchPage();
  // Add a new csar
  var btnStartCreateCsar = browser.element(by.binding('CSAR.BUTTON_NEWCSAR'));
  btnStartCreateCsar.click();
  element(by.model('csar.name')).sendKeys(csarName);
  element(by.model('csar.version')).sendKeys(csarVersion);
  element(by.model('csar.description')).sendKeys(description);

  // Create an csar and find it in the list
  var btnCreate = browser.element(by.binding('CREATE'));
  browser.actions().click(btnCreate).perform();
  browser.waitForAngular();
};
module.exports.createCsar = createCsar;

/* Jump to 'elementNumber' element in csar list */
var goToCsarDetails = function(elementNumber) {
  goToCsarSearchPage();
  var csars = element.all(by.repeater('csar in csarSearchResult.data.data'));
  expect(csars.count()).toBeGreaterThan(elementNumber);
  // Select the first line and click on details button
  var firstElement = csars.get(elementNumber);
  firstElement.click();
};
module.exports.goToCsarDetails = goToCsarDetails;
