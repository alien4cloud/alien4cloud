package org.alien4cloud.tosca.normative.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ToscaFunctionConstants {

    /* possible functions */
    public static final String GET_PROPERTY = "get_property";
    public static final String GET_ATTRIBUTE = "get_attribute";
    public static final String GET_INPUT = "get_input";
    public static final String GET_OPERATION_OUTPUT = "get_operation_output";

    /* reserved keywords */
    public static final String SELF = "SELF";
    public static final String TARGET = "TARGET";
    public static final String SOURCE = "SOURCE";
    public static final String HOST = "HOST";
}
