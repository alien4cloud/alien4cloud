package alien4cloud.model.cloud;

import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("PMD.UnusedPrivateField")
public class NetworkTemplate implements ICloudResourceTemplate {

    @NotNull
    private String id;

    @NotNull
    private Integer ipVersion;

    @NotNull
    private Boolean isExternal;

    private String cidr;

    private String gatewayIp;

    private String description;
}
