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

var fillCsargitModalWithData=function(urlContent,subPath,branchId){
  element(by.model('csarGit.url')).sendKeys(urlContent);
  element(by.model('location.subPath')).sendKeys(subPath);
  element(by.model('location.branchId')).sendKeys(branchId);
};

/*Jump to the Csargit modal and fill its content with values passed in parameter*/
var checkIfCreationStepIsEnabled = function(urlContent,subPath,branchId){
  goToCsarSearchPage();
  var openModal = browser.element(by.binding('CSAR.MODAL_NEWCSAR'));
  openModal.click();
  browser.driver.switchTo().activeElement();
  fillCsargitModalWithData(urlContent,subPath,branchId);
  var addLocationButton =element(by.id('btn-createTextField'));
  addLocationButton.click();
  var createCsarGitButton = element(by.id('btn-create'));
  expect(createCsarGitButton.isEnabled()).toBe(true);
  createCsarGitButton.click();
};
module.exports.checkIfCreationStepIsEnabled = checkIfCreationStepIsEnabled;


var checkIfCreationIsDisabled = function(urlContent,subPath,branchId,createCsarGitBtn,addLocationBtn){
  goToCsarSearchPage();
  var openModal = browser.element(by.binding('CSAR.MODAL_NEWCSAR'));
  openModal.click();
  browser.driver.switchTo().activeElement();
  fillCsargitModalWithData(urlContent,subPath,branchId);
  var createCsarGitButton = element(by.id('btn-create'));
  expect(createCsarGitButton.isEnabled()).toBe(false);

};
module.exports.checkIfCreationIsDisabled = checkIfCreationIsDisabled;

var checkIfAllCreationStepsAreDisabled = function(urlContent,subPath,branchId){
  goToCsarSearchPage();
  var openModal = browser.element(by.binding('CSAR.MODAL_NEWCSAR'));
  openModal.click();
  browser.driver.switchTo().activeElement();
  fillCsargitModalWithData(urlContent,subPath,branchId);
  var addLocationButton =element(by.id('btn-createTextField'));
  expect(addLocationButton.isEnabled()).toBe(false);
  var createCsarGitButton = element(by.id('btn-create'));
  expect(createCsarGitButton.isEnabled()).toBe(false);

};
module.exports.checkIfAllCreationStepsAreDisabled = checkIfAllCreationStepsAreDisabled;


module.exports.goToCsarDetails = goToCsarDetails;
