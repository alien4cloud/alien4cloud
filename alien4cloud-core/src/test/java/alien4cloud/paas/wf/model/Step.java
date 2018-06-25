package alien4cloud.paas.wf.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

/**
 * Created by xdegenne on 22/06/2018.
 */
@Getter
@Setter
public class Step {
    String node;
    String relation;
    String interf;
    String operation;
    String state;
    Set<String> to;
}
