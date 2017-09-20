package org.alien4cloud.tosca.model.definitions;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * Represents a time interval data structure in TOSCA. Note that Time interval is not a primitive type but is used in policy trigger.
 */
@Getter
@Setter
public class TimeInterval {
    private String startTime;
    private String endTime;
}