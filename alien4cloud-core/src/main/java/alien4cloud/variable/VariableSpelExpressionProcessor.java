package alien4cloud.variable;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

public class VariableSpelExpressionProcessor {

    private SpelExpressionParser parser;
    private ParserContext templateContext;
    private EvaluationContext context;

    public VariableSpelExpressionProcessor(VariableResolver resolver) {
        SpelParserConfiguration configuration = new SpelParserConfiguration(true, true);
        parser = new SpelExpressionParser(configuration);
        templateContext = new TemplateParserContext();
        context = new AlienVariableEvaluationContext(resolver);
    }

    public <T> T process(String expressionString, Class<T> clazz) {
        Expression expression = parser.parseExpression(expressionString, templateContext);
        return expression.getValue(context, clazz);
    }

    private static class AlienVariableEvaluationContext extends StandardEvaluationContext {
        private VariableResolver resolver;

        public AlienVariableEvaluationContext(VariableResolver resolver) {
            this.resolver = resolver;
        }

        @Override
        public Object lookupVariable(String name) {
            Object variable = super.lookupVariable(name);
            if (variable != null) {
                return variable;
            } else {
                return resolver.resolve(name, Object.class);
            }
        }
    }
}
