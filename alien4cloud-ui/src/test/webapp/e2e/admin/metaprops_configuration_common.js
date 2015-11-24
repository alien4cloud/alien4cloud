/* global by, expect, browser, element */
'use strict';

var common = require('../common/common');
var xedit = require('../common/xedit');
var genericForm = require('../generic_form/generic_form');

var constraintsMap = {
  equal: 'CONSTRAINT.EQUAL',
  greaterThan: 'CONSTRAINT.GREATER_THAN',
  greaterOrEqual: 'CONSTRAINT.GREATER_OR_EQUAL',
  lessThan: 'CONSTRAINT.LESS_THAN',
  lessOrEqual: 'CONSTRAINT.LESS_OR_EQUAL',
  inRange: 'CONSTRAINT.IN_RANGE',
  length: 'CONSTRAINT.LENGTH',
  maxLength: 'CONSTRAINT.MAX_LENGTH',
  minLength: 'CONSTRAINT.MIN_LENGTH',
  pattern: 'CONSTRAINT.PATTERN',
  validValues: 'CONSTRAINT.VALID_VALUES'
};
module.exports.constraintsMap = constraintsMap;

// TAGS BASIC OBJECTS
var maturityTag = {
  name: {
    'field': 'name',
    'ui': 'xeditable',
    'value': '_ALIEN_MATURITY'
  },
  description: {
    'field': 'description',
    'ui': 'xeditable',
    'value': 'The maturity of a component'
  },
  required: {
    'field': 'required',
    'ui': 'radio',
    'value': 'true'
  },
  target: {
    'field': 'target',
    'ui': 'select',
    'value': 'application'
  },
  type: {
    'field': 'type',
    'ui': 'select',
    'value': 'string'
  },
  default: {
    'field': 'default',
    'ui': 'xeditable',
    'value': 'Milestone maturity...'
  }
};
module.exports.maturityTag = maturityTag;

var tagValidValues = {
  name: {
    'field': 'name',
    'ui': 'xeditable',
    'value': '_ALIEN_RELEASE_VALID_VALUES'
  },
  description: {
    'field': 'description',
    'ui': 'xeditable',
    'value': 'My description'
  },
  required: {
    'field': 'required',
    'ui': 'radio',
    'value': 'true'
  },
  target: {
    'field': 'target',
    'ui': 'select',
    'value': 'application'
  },
  type: {
    'field': 'type',
    'ui': 'select',
    'value': 'string'
  },
  default: {
    'field': 'default',
    'ui': 'xeditable',
    'value': 'Q3'
  }
};
module.exports.tagValidValues = tagValidValues;

var tagMinLength = {
  name: {
    'field': 'name',
    'ui': 'xeditable',
    'value': '_ALIEN_PASSWORD_MIN4'
  },
  description: {
    'field': 'description',
    'ui': 'xeditable',
    'value': 'Password field with min string lenght of 4'
  },
  required: {
    'field': 'required',
    'ui': 'radio',
    'value': 'false'
  },
  target: {
    'field': 'target',
    'ui': 'select',
    'value': 'application'
  },
  type: {
    'field': 'type',
    'ui': 'select',
    'value': 'string'
  },
  password: {
    'field': 'password',
    'ui': 'radio',
    'value': 'true'
  },
  default: {
    'field': 'default',
    'ui': 'xeditable',
    'value': ''
  }
};
module.exports.tagMinLength = tagMinLength;

var defaultCloudProperty = {
  name: {
    'field': 'name',
    'ui': 'xeditable',
    'value': 'distribution'
  },
  description: {
    'field': 'description',
    'ui': 'xeditable',
    'value': 'My description'
  },
  required: {
    'field': 'required',
    'ui': 'radio',
    'value': 'false'
  },
  target: {
    'field': 'target',
    'ui': 'select',
    'value': 'cloud'
  },
  type: {
    'field': 'type',
    'ui': 'select',
    'value': 'string'
  },
  default: {
    'field': 'default',
    'ui': 'xeditable',
    'value': ''
  }
};
module.exports.defaultCloudProperty = defaultCloudProperty;

// CONSTRAINTS
var tagValidValuesConstraint = [{
  constraint: constraintsMap.validValues,
  value: ['Q1', 'Q2', 'Q3', 'Q4']
}];
module.exports.tagValidValuesConstraint = tagValidValuesConstraint;

var tagMaturityValidValuesConstraint = [{
  constraint: constraintsMap.validValues,
  value: ['M1', 'M2', 'M2']
}];
module.exports.tagMaturityValidValuesConstraint = tagMaturityValidValuesConstraint;

var tagMinLengthConstraint = [{
  constraint: constraintsMap.minLength,
  value: 4
}];
module.exports.tagMinLengthConstraint = tagMinLengthConstraint;

function selectConstraint(selectNumberId, constraintType) {
  var selectElement = common.element(by.id('abstractTypeFormLabelconstraints' + selectNumberId + 'selectImplementation'));
  common.click(by.id(constraintType), selectElement);
}

// Action to be executed before each topology test
var addTagConfiguration = function(tagConfigObject, tagConstraints) {
  common.click(by.id('menu.admin'));
  common.click(by.id('am.admin.metaprops'));

  // Open add node type form button
  var createTagConfigurationForm = element(by.id('createTagConfigurationForm'));
  var tagConfigurationsTable = element(by.id('tagConfigurationsTable'));

  expect(createTagConfigurationForm.isPresent()).toBe(false);
  expect(tagConfigurationsTable.isPresent()).toBe(true);

  // Open create node type form
  common.click(by.binding('TAG_CONFIG.CREATE'));
  expect(createTagConfigurationForm.isPresent()).toBe(true);
  expect(createTagConfigurationForm.isDisplayed()).toBe(true);

  var elemObject = null;
  if (tagConfigObject !== {}) {
    Object.keys(tagConfigObject).forEach(function(element) {
      elemObject = tagConfigObject[element];
      genericForm.sendValueToPrimitive(elemObject.field, elemObject.value, false, elemObject.ui);
    });
  }

  // CONSTRAINTS HANDLE
  if (tagConstraints !== null && tagConstraints.length > 0) {

    // click 'Constraints' button
    common.click(by.id('arrayTypeFormLabelconstraintseditbutton'));

    for (var i = 0; i < tagConstraints.length; i++) {
      // explode the current constraint
      var constraintType = tagConstraints[i].constraint;
      var constraintValue = tagConstraints[i].value;

      // click + add constraint
      common.click(by.id('arrayTypeFormconstraintsaddelementbutton'));

      var editField;
      switch (constraintType) {

        case constraintsMap.validValues: // VALID VALUES
          // select the good type for the current constraint select
          selectConstraint(i, constraintType);
          // first > for validValues constraint
          common.click(by.id('abstractTypeFormLabelconstraints' + i + 'editbutton'));
          // second > for validValues constraint
          common.click(by.id('arrayTypeFormLabelconstraints' + i + 'validValueseditbutton'));
          // enter all valid values
          for (var j = 0; j < constraintValue.length; j++) {
            // + add new value
            common.click(by.id('arrayTypeFormconstraints' + i + 'validValuesaddelementbutton'));
            // enter value
            editField = 'primitiveTypeFormLabelconstraints' + i + 'validValues' + j + 'input';
            xedit.sendKeys(editField, constraintValue[j], false);
          }
          break;
        case constraintsMap.minLength: // min string LENGTH
          // select the good type for the current constraint select
          selectConstraint(i, constraintType);
          // click > to add the value
          common.click(by.id('abstractTypeFormLabelconstraints' + i + 'editbutton'));
          // enter the value
          editField = 'primitiveTypeFormLabelconstraints' + i + 'minLengthinput';
          xedit.sendKeys(editField, constraintValue, false);
          break;
        case constraintsMap.maxLength: // max string LENGTH
          // select the good type for the current constraint select
          selectConstraint(i, constraintType);
          // click > to add the value
          common.click(by.id('abstractTypeFormLabelconstraints' + i + 'editbutton'));
          // enter the value
          editField = 'primitiveTypeFormLabelconstraints' + i + 'maxLengthinput';
          xedit.sendKeys(editField, constraintValue, false);
          break;
        default:
          console.error('CONSTRAINT NOT FOUND');
      }
    }
  }
  // Save
  genericForm.saveForm();
  tagConfigurationsTable = common.element(by.id('tagConfigurationsTable'));
  expect(tagConfigurationsTable.isDisplayed()).toBe(true);
};
module.exports.addTagConfiguration = addTagConfiguration;

var createConfigurationTags = function() {
  // Add 2 tags
  addTagConfiguration(tagValidValues, tagValidValuesConstraint);
  addTagConfiguration(tagMinLength, tagMinLengthConstraint);
};
module.exports.createConfigurationTags = createConfigurationTags;

var clickFirstElementInTagList = function(expectedName) {
  var tagList = common.element(by.id('tagConfigurationsTable'));
  expect(tagList.all(by.tagName('tr')).count()).toEqual(1);
  var firstTag = tagList.all(by.tagName('tr')).first();
  expect(firstTag.all(by.tagName('td')).first().getText()).toContain(expectedName);
  // click on the element.
  browser.actions().click(firstTag).perform();
  browser.waitForAngular();
  return firstTag;
};
module.exports.clickFirstElementInTagList = clickFirstElementInTagList;

var editTagConfiguration = function(propertyName, propertyValue) {
  var propertyElement = common.element(by.id('p_' + propertyName));
  expect(propertyElement.isPresent()).toBe(true);
  common.click(by.tagName('span'), propertyElement);

  var editForm = common.element(by.tagName('form'), propertyElement);
  var inputValue = common.element(by.tagName('input'), editForm);

  inputValue.clear();
  inputValue.sendKeys(propertyValue);
  editForm.submit();
  browser.waitForAngular();
};
module.exports.editTagConfiguration = editTagConfiguration;

// check if a text is present in the error message while editing a property
var checkTagEditionError = function(propertyName, containedInErrorText) {
  var propertyElement = common.element(by.id('p_' + propertyName));
  var formElement = common.element(by.tagName('form'), propertyElement);

  // getting error div under the input
  var divError = common.element(by.tagName('div'), formElement);
  expect(divError.isDisplayed()).toBe(true);
  expect(divError.getText()).not.toEqual('');
  expect(divError.getText()).toContain(containedInErrorText);
};
module.exports.checkTagEditionError = checkTagEditionError;
