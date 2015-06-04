package alien4cloud.rest.topology;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Information about a topology related resource
 * 
 * @author cem
 *
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CsarRelatedResourceDTO {
	private String resourceName;
	private String resourceType;
	private String resourceId;
}
