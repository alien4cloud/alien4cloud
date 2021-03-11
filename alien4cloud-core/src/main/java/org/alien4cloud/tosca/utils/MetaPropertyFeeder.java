package org.alien4cloud.tosca.utils;

import alien4cloud.common.MetaPropertiesService;
import alien4cloud.model.common.IMetaProperties;
import alien4cloud.model.common.MetaPropConfiguration;
import alien4cloud.model.common.MetaPropertyTarget;
import alien4cloud.model.common.Tag;
import alien4cloud.utils.services.ConstraintPropertyService;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.alien4cloud.tosca.exceptions.ConstraintValueDoNotMatchPropertyTypeException;
import org.alien4cloud.tosca.exceptions.ConstraintViolationException;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MetaPropertyFeeder {

    /**
     * If this prefix is found in TOSCA metadata name, A4C will try to find and feed the corresponding meta-property for NodeType.
     */
    public static final String A4C_METAPROPERTY_PREFIX = "A4C_META_";

    @Inject
    private MetaPropertiesService metaPropertiesService;

    public void feed(IMetaProperties newElement, List<Tag> tags, Map<String, MetaPropConfiguration> metapropsByNames) {
        if (tags == null) {
            return;
        }
        Map<String, String> metaProperties = newElement.getMetaProperties();
        if (metaProperties == null) {
            metaProperties = Maps.newHashMap();
            newElement.setMetaProperties(metaProperties);
        }
        Set<String> tagsToRemove = Sets.newHashSet();

        Iterator<Tag> tagIterator = tags.iterator();
        while (tagIterator.hasNext()) {
            Tag tag = tagIterator.next();
            if (tag.getName().startsWith(A4C_METAPROPERTY_PREFIX)) {
                String metapropertyName = tag.getName().substring(A4C_METAPROPERTY_PREFIX.length());
                MetaPropConfiguration metaPropConfig = metapropsByNames.get(metapropertyName);
                if (metaPropConfig != null) {
                    // validate tag value using meta prop constraints
                    try {
                        ConstraintPropertyService.checkPropertyConstraint(metaPropConfig.getId(), tag.getValue(), metaPropConfig);
                        metaProperties.put(metaPropConfig.getId(), tag.getValue());
                        tagIterator.remove();
                    } catch (ConstraintValueDoNotMatchPropertyTypeException e) {
                        // TODO: manage error
                        // for the moment the error is ignored, but the meta-property is not set and the
                        // tag not removed, so the user can easily guess that something gone wrong ...
                    } catch (ConstraintViolationException e) {
                        // TODO: manage error
                    }
                }
            }
        }
    }

    public Map<String,Object> buildContext() {
        Map<String,String> map = metaPropertiesService.getMetaPropConfigurationsByName(MetaPropertyTarget.TOPOLOGY)
                .values().stream().collect(Collectors.toMap(MetaPropConfiguration::getId,MetaPropConfiguration::getName));

        Map<String,Object> ctx = Maps.newHashMap();

        ctx.put("metaResolver",new Function<String,String>() {
            @Override
            public String apply(String key) {
                return map.getOrDefault(key,key);
            }
        });
        return ctx;
    }
}
