package org.alien4cloud.server;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for maintenance mode rest update.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
public class MaintenanceUpdateDTO {
    /** Optional message to display to users */
    private String message;
    /** Optional completion percentage of the maintenance. */
    private Integer progressPercentage;
}