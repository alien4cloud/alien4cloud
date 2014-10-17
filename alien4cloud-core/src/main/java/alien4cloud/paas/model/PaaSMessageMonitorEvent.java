package alien4cloud.paas.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.elasticsearch.annotation.ESObject;

@Getter
@Setter
@ESObject
@ToString(callSuper = true)
@SuppressWarnings("PMD.UnusedPrivateField")
public class PaaSMessageMonitorEvent extends AbstractMonitorEvent {
    private String message;
}
