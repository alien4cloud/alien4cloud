package org.alien4cloud.tosca.utils;

import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.Resource;

import java.util.Map;
import java.util.Properties;

public class PropertiesYamlParser {

    public static class ToProperties {
        private ToProperties() {
        }

        public static Properties from(Resource resource) {
            YamlWithIntermediateValuePropertiesFactoryBean factoryBean = new YamlWithIntermediateValuePropertiesFactoryBean();
            factoryBean.setResources(resource);
            return factoryBean.getObject();
        }
    }

    public static class ToMap {
        private ToMap() {
        }

        public static Map<String, Object> from(Resource resource) {
            YamlMapFactoryBean factoryBean = new YamlMapFactoryBean();
            factoryBean.setResources(resource);
            return factoryBean.getObject();
        }
    }

    private static class YamlWithIntermediateValuePropertiesFactoryBean extends YamlPropertiesFactoryBean {

        /**
         * The original {@link YamlPropertiesFactoryBean} include only the leaf of each Yaml entry as property.
         * <p>
         * i.e:
         * yamlProperties:
         * subfield:
         * field: value
         * <p>
         * With the original version only "yamlProperties.subfield.field" is accessible.
         * With the new implementation "yamlProperties.subfield.field" AND "yamlProperties.subfield" are accessible.
         * "yamlProperties.subfield" contains a Map.of("field", "value)
         * <p>
         * Please note, if a property name contains dot like below:
         * <p>
         * yamlProperties:
         * subfield.with.dot:
         * field: value
         * <p>
         * With the original version only "yamlProperties.subfield.with.dot.field" is accessible.
         * With the new implementation  "yamlProperties.subfield.with.dot" AND "yamlProperties.subfield.with.dot.field" are accessible.
         * "yamlProperties.subfield.with" is NOT accessible.
         *
         * @return a list of property that include leaf values and intermediate values
         */
        @Override
        protected Properties createProperties() {
            final Properties result = new Properties();
            process((properties, map) -> {
                result.putAll(properties);
                processMap("", result, map);
            });
            return result;
        }

        private void processMap(String parentKey, Properties result, Map<String, Object> map) {
            map.forEach((k, v) -> {
                result.put(parentKey + k, v);

                if (Map.class.isAssignableFrom(v.getClass())) {
                    processMap(parentKey + k + ".", result, (Map<String, Object>) v);
                }
            });
        }
    }

}
