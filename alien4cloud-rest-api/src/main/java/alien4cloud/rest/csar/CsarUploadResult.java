package alien4cloud.rest.csar;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.alien4cloud.tosca.model.Csar;
import alien4cloud.tosca.parser.ParsingError;

import com.google.common.collect.Maps;

/**
 * Result of a parsing.
 */
@Getter
@Setter
@NoArgsConstructor
public class CsarUploadResult {
    private Csar csar;
    private Map<String, List<ParsingError>> errors = Maps.newHashMap();
}