Feature: suggestion on a value backed by a property definition

  Background:
    Given I am authenticated with "ADMIN" role
    And I initialize default suggestions entry
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"

  @reset
  Scenario: Get all suggestions
    When I get all suggestions for property "architecture" of "capability" "tosca.capabilities.OperatingSystem"
    Then I should receive a RestResponse with no error
    And The RestResponse should contain 2 element(s):
      | x86_32 |
      | x86_64 |
    When I get all suggestions for property "type" of "capability" "tosca.capabilities.OperatingSystem"
    Then I should receive a RestResponse with no error
    And The RestResponse should contain 4 element(s):
      | linux   |
      | aix     |
      | mac     |
      | windows |
    When I get all suggestions for property "distribution" of "capability" "tosca.capabilities.OperatingSystem"
    Then I should receive a RestResponse with no error
    And The RestResponse should contain 10 element(s):
      | debian              |
      | fedora              |
      | rhel                |
      | ubuntu              |
      | centos              |
      | gentoo              |
      | windows server 2003 |
      | windows server 2008 |
      | windows server 2012 |
      | windows server 2016 |

  @reset
  Scenario: Get suggestions which match
    When I get suggestions for text "x86_32" for property "architecture" of "capability" "tosca.capabilities.OperatingSystem"
    Then I should receive a RestResponse with no error
    And The RestResponse should contain 2 element(s) in this order:
      | x86_32 |
      | x86_64 |
    When I get suggestions for text "x86_64" for property "architecture" of "capability" "tosca.capabilities.OperatingSystem"
    Then I should receive a RestResponse with no error
    And The RestResponse should contain 2 element(s) in this order:
      | x86_64 |
      | x86_32 |
    When I get suggestions for text "Kubuntoo" for property "distribution" of "capability" "tosca.capabilities.OperatingSystem"
    Then I should receive a RestResponse with no error
    And The RestResponse should contain 2 element(s) in this order:
      | ubuntu |
      | gentoo |
    When I get suggestions for text "w" for property "type" of "capability" "tosca.capabilities.OperatingSystem"
    Then I should receive a RestResponse with no error
    And The RestResponse should contain 1 element(s) in this order:
      | windows |

  @reset
  Scenario: Add suggestion
    When I add suggestion "kubuntu" for property "distribution" of "capability" "tosca.capabilities.OperatingSystem"
    Then I should receive a RestResponse with no error
    When I get suggestions for text "Kubuntu" for property "distribution" of "capability" "tosca.capabilities.OperatingSystem"
    Then I should receive a RestResponse with no error
    And The RestResponse should contain 2 element(s) in this order:
      | kubuntu |
      | ubuntu  |

  @reset
  Scenario: I upload an archive with a wrong capability property value
    When I upload the archive "tosca-normative-types"
    And I upload the archive "topology with wrong os distribution value"
    Then I should receive a RestResponse with 2 alerts in 1 files : 0 errors 1 warnings and 1 infos
    When I get suggestions for text "ubuntoo" for property "distribution" of "capability" "tosca.capabilities.OperatingSystem"
    Then I should receive a RestResponse with no error
    And The RestResponse should contain 2 element(s) in this order:
      | ubuntu |
      | gentoo |

  @reset
  Scenario: I upload an archive with a similar capability property value
    When I upload the archive "tosca-normative-types"
    And I upload the archive "topology with similar os distribution value"
    Then I should receive a RestResponse with 2 alerts in 1 files : 0 errors 0 warnings and 2 infos
    When I get suggestions for text "kubuntu" for property "distribution" of "capability" "tosca.capabilities.OperatingSystem"
    Then I should receive a RestResponse with no error
    And The RestResponse should contain 2 element(s) in this order:
      | Kubuntu |
      | ubuntu  |

  @reset
  Scenario: I upload an archive with a wrong property value
    Given I create suggestion for property "device" of "node" "tosca.nodes.BlockStorage" with initial values "/dev/vdb"
    When I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I upload the archive "topology with wrong device value"
    Then I should receive a RestResponse with 2 alerts in 1 files : 0 errors 1 warnings and 1 infos
    When I get suggestions for text "/dev" for property "device" of "node" "tosca.nodes.BlockStorage"
    Then I should receive a RestResponse with no error
    And The RestResponse should contain 1 element(s) in this order:
      | /dev/vdb |

  @reset
  Scenario: I upload an archive with a similar property value
    Given I create suggestion for property "device" of "node" "tosca.nodes.BlockStorage" with initial values "/dev/vdb"
    When I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I upload the archive "topology with similar device value"
    Then I should receive a RestResponse with 2 alerts in 1 files : 0 errors 0 warnings and 2 infos
    When I get suggestions for text "/dev/vdb" for property "device" of "node" "tosca.nodes.BlockStorage"
    Then I should receive a RestResponse with no error
    And The RestResponse should contain 2 element(s) in this order:
      | /dev/vdb |
      | /dev/vdc |

  @reset
  Scenario: I upload an archive with a wrong relationship property value
    And I upload the archive "containing relationship types for suggestion tests"
    Given I create suggestion for property "install_dir" of "relationship" "alien.test.SoftwareHostedOnCompute" with initial values "/opt/software1"
    When I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I upload the archive "topology with wrong relationship property value"
    Then I should receive a RestResponse with 2 alerts in 1 files : 0 errors 1 warnings and 1 infos
    When I get suggestions for text "/opt/software1" for property "install_dir" of "relationship" "alien.test.SoftwareHostedOnCompute"
    Then I should receive a RestResponse with no error
    And The RestResponse should contain 1 element(s) in this order:
      | /opt/software1 |

  @reset
  Scenario: I upload an archive with a similar relationship property value
    And I upload the archive "containing relationship types for suggestion tests"
    Given I create suggestion for property "install_dir" of "relationship" "alien.test.SoftwareHostedOnCompute" with initial values "/opt/software1"
    When I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I upload the archive "topology with similar relationship property value"
    Then I should receive a RestResponse with 2 alerts in 1 files : 0 errors 0 warnings and 2 infos
    When I get suggestions for text "/opt/software1" for property "install_dir" of "relationship" "alien.test.SoftwareHostedOnCompute"
    Then I should receive a RestResponse with no error
    And The RestResponse should contain 2 element(s) in this order:
      | /opt/software1 |
      | /opt/software2 |

  @reset
  Scenario: I upload an archive with a wrong node filter constraint value
    And I upload the archive "node type with wrong node filter constraint value"
    Then I should receive a RestResponse with 2 alerts in 1 files : 0 errors 2 warnings and 0 infos
    When I get suggestions for text "linux" for property "type" of "capability" "tosca.capabilities.OperatingSystem"
    Then I should receive a RestResponse with no error
    And The RestResponse should contain 2 element(s) in this order:
      | linux   |
      | windows |
    When I get suggestions for text "x86_32" for property "architecture" of "capability" "tosca.capabilities.OperatingSystem"
    Then I should receive a RestResponse with no error
    And The RestResponse should contain 2 element(s) in this order:
      | x86_32 |
      | x86_64 |

  @reset
  Scenario: I upload an archive with a similar node filter constraint value
    And I upload the archive "node type with similar node filter constraint value"
    Then I should receive a RestResponse with 3 alerts in 1 files : 0 errors 0 warnings and 3 infos
    When I get suggestions for text "linux" for property "type" of "capability" "tosca.capabilities.OperatingSystem"
    Then I should receive a RestResponse with no error
    And The RestResponse should contain 2 element(s) in this order:
      | linux  |
      | linuxx |
    When I get suggestions for text "x86_32" for property "architecture" of "capability" "tosca.capabilities.OperatingSystem"
    Then I should receive a RestResponse with no error
    And The RestResponse should contain 2 element(s) in this order:
      | x86_32  |
      | x86_322 |

  @reset
  Scenario: I upload an archive with a new node filter constraint value for equal constraint, that will trigger the creation of new suggestion
    And I upload the archive "node type with new suggestion for equal constraint of node filter"
    When I get suggestions for text "14" for property "version" of "capability" "tosca.capabilities.OperatingSystem"
    Then I should receive a RestResponse with no error
    And The RestResponse should contain 1 element(s) in this order:
      | 14.04 |

  @reset
  Scenario: I upload an archive with a new node filter constraint value for valid values constraint, that will trigger the creation of new suggestion
    And I upload the archive "node type with new suggestion for valid values constraint of node filter"
    When I get suggestions for text "14.04" for property "version" of "capability" "tosca.capabilities.OperatingSystem"
    Then I should receive a RestResponse with no error
    And The RestResponse should contain 2 element(s) in this order:
      | 14.04  |
      | 14.042 |
