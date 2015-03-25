Feature: Audit log

  Background:
    Given I am authenticated with user named "admin"
    And I reset audit log configuration

  Scenario: Creating applications should generate log audit
    When I create a new application with name "watchmiddleearth" and description "Use my great eye to find frodo and the ring."
    Then I should have 1 audit traces in Alien:
      | admin | Application | create |
    When I delete the application "watchmiddleearth"
    Then I should have 2 audit traces in Alien:
      | admin | Application | create |
      | admin | Application | delete |

  Scenario: Audit Log configuration can be retrieved
    When I get audit log configuration
    Then I should have audit log enabled globally
    And I should have audit log enabled for:
      | Application | create |
      | Application | delete |

  Scenario: Disable/enable audit log globally
    When I disable audit log globally
    And I get audit log configuration
    Then I should have audit log disabled globally
    When I create a new application with name "watchmiddleearth" and description "Use my great eye to find frodo and the ring."
    Then I should have no audit trace in Alien
    When I enable audit log globally
    And I get audit log configuration
    Then I should have audit log enabled globally
    When I create a new application with name "watchmiddleearth" and description "Use my great eye to find frodo and the ring."
    Then I should have 1 audit traces in Alien:
      | admin | Application | create |

  Scenario: Disable/enable audit log per method
    When I disable audit log for following methods:
      | Application | delete |
    And I get audit log configuration
    Then I should have audit log disabled for:
      | Application | delete |
    When I create a new application with name "watchmiddleearth" and description "Use my great eye to find frodo and the ring."
    Then I should have 1 audit traces in Alien:
      | admin | Application | create |
    When I delete the application "watchmiddleearth"
    Then I should have 1 audit traces in Alien:
      | admin | Application | create |
    When I enable audit log for following methods:
      | Application | delete |
    And I create a new application with name "watchmiddleearth" and description "Use my great eye to find frodo and the ring."
    And I delete the application "watchmiddleearth"
    Then I should have 3 audit traces in Alien:
      | admin | Application | create |
      | admin | Application | create |
      | admin | Application | delete |

  Scenario: Rest audit log configuration
    When I disable audit log globally
    And I disable audit log for following methods:
      | Application | create |
      | Application | delete |
    And I reset audit log configuration
    And I get audit log configuration
    Then I should have audit log enabled globally
    And I should have audit log enabled for:
      | Application | create |
      | Application | delete |
