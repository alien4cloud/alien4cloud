[1mdiff --git a/alien4cloud-core/src/main/java/alien4cloud/paas/function/FunctionEvaluator.java b/alien4cloud-core/src/main/java/alien4cloud/paas/function/FunctionEvaluator.java[m
[1mindex b4a7abb..770c981 100644[m
[1m--- a/alien4cloud-core/src/main/java/alien4cloud/paas/function/FunctionEvaluator.java[m
[1m+++ b/alien4cloud-core/src/main/java/alien4cloud/paas/function/FunctionEvaluator.java[m
[36m@@ -1,16 +1,7 @@[m
 package alien4cloud.paas.function;[m
 [m
 import alien4cloud.common.AlienConstants;[m
[31m-import alien4cloud.model.components.AbstractPropertyValue;[m
[31m-import alien4cloud.model.components.AttributeDefinition;[m
[31m-import alien4cloud.model.components.ConcatPropertyValue;[m
[31m-import alien4cloud.model.components.FunctionPropertyValue;[m
[31m-import alien4cloud.model.components.IValue;[m
[31m-import alien4cloud.model.components.IndexedInheritableToscaElement;[m
[31m-import alien4cloud.model.components.IndexedToscaElement;[m
[31m-import alien4cloud.model.components.PropertyDefinition;[m
[31m-import alien4cloud.model.components.PropertyValue;[m
[31m-import alien4cloud.model.components.ScalarPropertyValue;[m
[32m+[m[32mimport alien4cloud.model.components.*;[m
 import alien4cloud.model.topology.Capability;[m
 import alien4cloud.model.topology.NodeTemplate;[m
 import alien4cloud.model.topology.Requirement;[m
[36m@@ -299,6 +290,20 @@[m [mpublic final class FunctionEvaluator {[m
         return null;[m
     }[m
 [m
[32m+[m[32m    private static String serializeComplexPropertyValue(Object value) {[m
[32m+[m[32m        try {[m
[32m+[m[32m            if (value instanceof String) {[m
[32m+[m[32m                return (String) value;[m
[32m+[m[32m            } else if (value instanceof  ComplexPropertyValue) {[m
[32m+[m[32m                return JsonUtil.toString(((ComplexPropertyValue) value).getValue());[m
[32m+[m[32m            } else {[m
[32m+[m[32m                return JsonUtil.toString(value);[m
[32m+[m[32m            }[m
[32m+[m[32m        } catch (JsonProcessingException e) {[m
[32m+[m[32m            return null;[m
[32m+[m[32m        }[m
[32m+[m[32m    }[m
[32m+[m
     private static String getPropertyValue(Map<String, AbstractPropertyValue> properties, Map<String, PropertyDefinition> propertyDefinitions,[m
             String propertyAccessPath) {[m
         if (properties == null || !properties.containsKey(propertyAccessPath)) {[m
[36m@@ -325,15 +330,7 @@[m [mpublic final class FunctionEvaluator {[m
                         throw new NotSupportedException("Only support static value in a get_property");[m
                     }[m
                     Object value = MapUtil.get(((PropertyValue) rawValue).getValue(), propertyAccessPath.substring(propertyName.length() + 1));[m
[31m-                    if (value instanceof String) {[m
[31m-                        return (String) value;[m
[31m-                    } else {[m
[31m-                        try {[m
[31m-                            return JsonUtil.toString(value);[m
[31m-                        } catch (JsonProcessingException e) {[m
[31m-                            return null;[m
[31m-                        }[m
[31m-                    }[m
[32m+[m[32m                    return serializeComplexPropertyValue(value);[m
                 } else {[m
                     return null;[m
                 }[m
[36m@@ -394,7 +391,11 @@[m [mpublic final class FunctionEvaluator {[m
                 }[m
             }[m
 [m
[31m-            return getScalarValue(propertyValue);[m
[32m+[m[32m            if (propertyValue instanceof ComplexPropertyValue) {[m
[32m+[m[32m                return serializeComplexPropertyValue(((ComplexPropertyValue) propertyValue).getValue());[m
[32m+[m[32m            } else {[m
[32m+[m[32m                return getScalarValue(propertyValue);[m
[32m+[m[32m            }[m
         }[m
 [m
         log.warn("The keyword <" + ToscaFunctionConstants.SELF[m
[1mdiff --git a/alien4cloud-tosca/src/main/java/alien4cloud/tosca/parser/impl/advanced/ImportParser.java b/alien4cloud-tosca/src/main/java/alien4cloud/tosca/parser/impl/advanced/ImportParser.java[m
[1mindex c742407..3845347 100644[m
[1m--- a/alien4cloud-tosca/src/main/java/alien4cloud/tosca/parser/impl/advanced/ImportParser.java[m
[1m+++ b/alien4cloud-tosca/src/main/java/alien4cloud/tosca/parser/impl/advanced/ImportParser.java[m
[36m@@ -2,6 +2,7 @@[m [mpackage alien4cloud.tosca.parser.impl.advanced;[m
 [m
 import javax.annotation.Resource;[m
 [m
[32m+[m[32mimport com.google.common.collect.Sets;[m
 import lombok.extern.slf4j.Slf4j;[m
 import org.springframework.stereotype.Component;[m
 import org.yaml.snakeyaml.nodes.Node;[m
[36m@@ -17,6 +18,8 @@[m [mimport alien4cloud.tosca.parser.impl.ErrorCode;[m
 import alien4cloud.tosca.parser.impl.base.ScalarParser;[m
 import alien4cloud.tosca.parser.mapping.DefaultParser;[m
 [m
[32m+[m[32mimport java.util.Set;[m
[32m+[m
 @Slf4j[m
 @Component[m
 public class ImportParser extends DefaultParser<CSARDependency> {[m
[36m@@ -33,6 +36,14 @@[m [mpublic class ImportParser extends DefaultParser<CSARDependency> {[m
             String[] dependencyStrs = valueAsString.split(":");[m
             if (dependencyStrs.length == 2) {[m
                 CSARDependency dependency = new CSARDependency(dependencyStrs[0], dependencyStrs[1]);[m
[32m+[m
[32m+[m[32m                if (ToscaContext.get() == null) {[m
[32m+[m[32m                    Set<CSARDependency> dependencies = Sets.newConcurrentHashSet();[m
[32m+[m[32m                    dependencies.add(dependency);[m
[32m+[m[32m                    log.debug("Initializing Tosca Context with dependencies {}", dependencies);[m
[32m+[m[32m                    ToscaContext.init(dependencies);[m
[32m+[m[32m                }[m
[32m+[m
                 Csar csar = ToscaContext.get().getArchive(dependency.getName(), dependency.getVersion());[m
                 log.info("Import {} {} {}", dependency.getName(), dependency.getVersion(), csar);[m
                 if (csar == null) {[m
