package alien4cloud.paas.wf.SUPALIEN_1202;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParsedTopology {

    private String _id;
    private ParsedTopologySource _source;

}
