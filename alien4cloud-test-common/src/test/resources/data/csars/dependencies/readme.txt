Dependencies tests archives aims to perform validations on alien4cloud behavior when transitive dependencies are defined.

# Tests to validate Alien4Cloud transitive dependency resolution

## Scenario 1:

Topology:
 - Av1
   - Cv1
 - Bv1
   - Dv1
     - Cv1

=> Everything fine, all dependencies should resolve as expected

## Scenario 2:

Topology:
 - Av1
   - Cv1
 - Bv2
   - Dv2
     - Cv2

=> Conflict on C dependency, resolves to 2

## Scenario 3:

Topology:
 - Av1
   - Cv1
 - Bv2
   - Dv2
     - Cv2
 - Cv1

=> Conflict on C dependency, resolves to 1

# Tests to validate errors due to archive changes and 'TOSCA invalid dependencies'

Note that while alien detect and behave correctly in case of missing types or invalid TOSCA relationships usages some errors may be related to script incompatibility or other version issues.

Missing type because of version switch
 - Minor incomaptibility (one of the two version seems to match)
 - Major incompatilibty (none of versions have types to work together)

## Scenario 4

Topology:
 - Av1
   - Cv1
 - Bv3
   - Dv3
     - Cv3

=> Conflict on C dependency, version 2 causes missing type, resolves to 1

## Scenario 5

Topology:
 - Av1
   - Cv1
 - Bv4
   - Dv4
     - Cv4
 
=> Conflict on C dependency, version 2 causes missing type, version 1 causes missing type => should fail



Node Filter incoherency behavior ? Default to ignore filter incoherency properties ?
