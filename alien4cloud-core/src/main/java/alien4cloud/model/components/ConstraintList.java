package alien4cloud.model.components;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.google.common.collect.Lists;

@Getter
@Setter
@NoArgsConstructor
public class ConstraintList {

    // @JsonDeserialize(contentUsing = PropertyConstraintDeserializer.class)
    private List<PropertyConstraint> constraints = Lists.newArrayList();
}
