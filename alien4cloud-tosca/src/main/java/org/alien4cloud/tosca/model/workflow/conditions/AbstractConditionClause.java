package org.alien4cloud.tosca.model.workflow.conditions;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * Abstract class for condition clauses.
 */
@Getter
@Setter
public class AbstractConditionClause {
    /** Defines an and clause where every defined clauses has to be considered truly so the clause is considered truly. */
    @Getter
    @Setter
    public static class AndConditionClause extends AbstractConditionClause {
        private List<AbstractConditionClause> clauses;
    }

    /** Defines an or clause where one of the defined clauses has to be considered truly so the clause is considered truly. */
    @Getter
    @Setter
    public static class OrConditionClause extends AbstractConditionClause {
        private List<AbstractConditionClause> clauses;
    }

    /**
     * Defines a clause with assertions against a node or relationship attributes. As in an and clause all assertions have to be truly so the assert clause is
     * considered truly.
     */
    @Getter
    @Setter
    public static class AssertConditionClause extends AbstractConditionClause {
        /** */
        private List<AssertionDefinition> assertions;
    }
}