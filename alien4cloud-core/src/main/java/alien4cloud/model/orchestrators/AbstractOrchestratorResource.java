package alien4cloud.model.orchestrators;

import static alien4cloud.dao.model.FetchContext.SUMMARY;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.annotation.NestedObject;
import org.elasticsearch.annotation.query.FetchContext;
import org.elasticsearch.annotation.query.TermFilter;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import alien4cloud.security.ISecurityEnabledResource;
import alien4cloud.utils.jackson.ConditionalAttributes;
import alien4cloud.utils.jackson.ConditionalOnAttribute;
import alien4cloud.utils.jackson.JSonMapEntryArrayDeSerializer;
import alien4cloud.utils.jackson.JSonMapEntryArraySerializer;
import alien4cloud.utils.jackson.NotAnalyzedTextMapEntry;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AbstractOrchestratorResource implements ISecurityEnabledResource {

    @TermFilter(paths = { "key", "value" })
    @NestedObject(nestedClass = NotAnalyzedTextMapEntry.class)
    @ConditionalOnAttribute(ConditionalAttributes.ES)
    @JsonDeserialize(using = JSonMapEntryArrayDeSerializer.class)
    @JsonSerialize(using = JSonMapEntryArraySerializer.class)
    @FetchContext(contexts = { SUMMARY }, include = { true })
    private Map<String, Set<String>> userPermissions;

    @TermFilter(paths = { "key", "value" })
    @NestedObject(nestedClass = NotAnalyzedTextMapEntry.class)
    @ConditionalOnAttribute(ConditionalAttributes.ES)
    @JsonDeserialize(using = JSonMapEntryArrayDeSerializer.class)
    @JsonSerialize(using = JSonMapEntryArraySerializer.class)
    @FetchContext(contexts = { SUMMARY }, include = { true })
    private Map<String, Set<String>> groupPermissions;

    @TermFilter(paths = { "key", "value" })
    @NestedObject(nestedClass = NotAnalyzedTextMapEntry.class)
    @ConditionalOnAttribute(ConditionalAttributes.ES)
    @JsonDeserialize(using = JSonMapEntryArrayDeSerializer.class)
    @JsonSerialize(using = JSonMapEntryArraySerializer.class)
    @FetchContext(contexts = { SUMMARY }, include = { true })
    private Map<String, Set<String>> applicationPermissions;

    @TermFilter(paths = { "key", "value" })
    @NestedObject(nestedClass = NotAnalyzedTextMapEntry.class)
    @ConditionalOnAttribute(ConditionalAttributes.ES)
    @JsonDeserialize(using = JSonMapEntryArrayDeSerializer.class)
    @JsonSerialize(using = JSonMapEntryArraySerializer.class)
    @FetchContext(contexts = { SUMMARY }, include = { true })
    private Map<String, Set<String>> environmentPermissions;

    private Map<String, Set<String>> getPermissions(String subjectType) {
        switch (subjectType) {
        case USER:
            return userPermissions == null ? new HashMap<>() : getUserPermissions();
        case GROUP:
            return groupPermissions == null ? new HashMap<>() : getGroupPermissions();
        case APPLICATION:
            return applicationPermissions == null ? new HashMap<>() : getApplicationPermissions();
        case ENVIRONMENT:
            return environmentPermissions == null ? new HashMap<>() : getEnvironmentPermissions();
        default:
            return new HashMap<>();
        }
    }

    private void setPermissions(String subjectType, Map<String, Set<String>> permissions) {
        switch (subjectType) {
        case USER:
            setUserPermissions(permissions);
        case GROUP:
            setGroupPermissions(permissions);
        case APPLICATION:
            setApplicationPermissions(permissions);
        case ENVIRONMENT:
            setEnvironmentPermissions(permissions);
        }
    }

    @Override
    public Set<String> getPermissions(String subjectType, String subject) {
        Set<String> permissions = getPermissions(subjectType).get(subject);
        return permissions == null ? new HashSet<>() : permissions;
    }

    @Override
    public void addPermissions(String subjectType, String subject, Set<String> permissions) {
        Map<String, Set<String>> allPermissions = getPermissions(subjectType);
        Set<String> newPermissions = allPermissions.get(subjectType);
        if (newPermissions == null) {
            newPermissions = new HashSet<>();
        }
        newPermissions.addAll(permissions);
        allPermissions.put(subject, newPermissions);
        setPermissions(subjectType, allPermissions);
    }

    @Override
    public void revokePermissions(String subjectType, String subject, Set<String> permissions) {
        getPermissions(subjectType, subject).removeAll(permissions);
    }

    @Override
    public void revokeAllPermissions(String subjectType, String subject) {
        getPermissions(subjectType).remove(subject);
    }
}
