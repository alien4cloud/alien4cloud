package org.alien4cloud.tosca.variable;

import org.springframework.core.env.PropertyResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * This class is responsible to configure and wrap the access to {@link SpelExpressionParser}
 * Variables are accessible within a SpEL expression thanks to {@link VariableEvaluationContext}
 */
public class SpelExpressionProcessor {

    private SpelExpressionParser parser;
    private ParserContext templateContext;
    private EvaluationContext context;

    public SpelExpressionProcessor(PropertyResolver resolver) {
        SpelParserConfiguration configuration = new SpelParserConfiguration(true, true);
        parser = new SpelExpressionParser(configuration);
        templateContext = new TemplateParserContext();
        context = new VariableEvaluationContext(resolver);
    }

    public <T> T process(String expressionString, Class<T> clazz) {
        if (expressionString == null) {
            return null;
        }

        Expression expression = parser.parseExpression(expressionString, templateContext);
        return expression.getValue(context, clazz);
    }

    private static class VariableEvaluationContext extends StandardEvaluationContext {
        private PropertyResolver resolver;

        private VariableEvaluationContext(PropertyResolver resolver) {
            this.resolver = resolver;
        }

        @Override
        public Object lookupVariable(String name) {
            Object variable = super.lookupVariable(name);
            if (variable != null) {
                return variable;
            } else {
                return resolver.getProperty(name, Object.class);
            }
        }
    }
}
