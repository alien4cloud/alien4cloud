package alien4cloud.rest.model;

import javax.validation.constraints.NotNull;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
public class SortConfiguration {
    @NotNull
    private String sortBy;
    private boolean ascending;
}
