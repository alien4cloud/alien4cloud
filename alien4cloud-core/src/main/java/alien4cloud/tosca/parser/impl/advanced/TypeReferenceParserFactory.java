package alien4cloud.tosca.parser.impl.advanced;

import javax.annotation.Resource;

import alien4cloud.tosca.parser.impl.base.ScalarParser;
import org.springframework.stereotype.Component;

import alien4cloud.component.model.IndexedInheritableToscaElement;
import alien4cloud.tosca.container.services.csar.ICSARRepositorySearchService;

@Component
public final class TypeReferenceParserFactory {
    @Resource
    private ICSARRepositorySearchService searchService;
    @Resource
    private ScalarParser scalarParser;

    public DerivedFromParser getDerivedFromParser(Class<? extends IndexedInheritableToscaElement> validType) {
        return new DerivedFromParser(searchService, scalarParser, validType);
    }

    @SafeVarargs
    public final TypeReferenceParser getTypeReferenceParser(Class<? extends IndexedInheritableToscaElement>... validTypes) {
        return new TypeReferenceParser(searchService, scalarParser, validTypes);
    }
}