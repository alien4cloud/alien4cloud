package alien4cloud.paas.wf;

import java.util.HashSet;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = Id.CLASS, include = As.PROPERTY, property = "type", visible = true)
public abstract class AbstractStep {

    // a unique name
    private String name;

    private Set<String> precedingSteps;
    private Set<String> followingSteps;

    public void addPreceding(String stepId) {
        if (precedingSteps == null) {
            precedingSteps = new HashSet<String>();
        }
        precedingSteps.add(stepId);
    }

    public void removePreceding(String stepId) {
        precedingSteps.remove(stepId);
    }

    public void addFollowing(String stepId) {
        if (followingSteps == null) {
            followingSteps = new HashSet<String>();
        }
        followingSteps.add(stepId);
    }

    public void removeFollowing(String stepId) {
        followingSteps.remove(stepId);
    }

    @JsonIgnore
    public abstract String getStepAsString();

    @Override
    public String toString() {
        return getStepAsString();
    }

}
