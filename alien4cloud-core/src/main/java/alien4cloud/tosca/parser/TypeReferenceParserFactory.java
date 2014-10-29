package alien4cloud.tosca.parser;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import alien4cloud.component.model.IndexedInheritableToscaElement;
import alien4cloud.tosca.container.services.csar.ICSARRepositorySearchService;
import alien4cloud.tosca.parser.impl.DerivedFromParser;
import alien4cloud.tosca.parser.impl.ScalarParser;
import alien4cloud.tosca.parser.impl.TypeReferenceParser;

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