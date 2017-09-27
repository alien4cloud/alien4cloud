package org.alien4cloud.tosca.variable;

import java.util.Arrays;
import java.util.List;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.env.PropertyResolver;
import org.springframework.expression.*;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeLocator;

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

            // forbidden to load any classes except default java classes
            // #{ T(org.springframework.security.core.context.SecurityContextHolder).getContext() }
            // won't be possible
            StandardTypeLocator standardTypeLocator = new StandardTypeLocator(getRootClassloader());
            setTypeLocator(standardTypeLocator);

            // disable any method calls
            // i.e.
            // #{ T(java.lang.Math).random() }
            // random() won't be resolve
            setMethodResolvers(Arrays.asList(new MethodResolver() {
                @Override
                public MethodExecutor resolve(EvaluationContext context, Object targetObject, String name, List<TypeDescriptor> argumentTypes) throws AccessException {
                    return null;
                }
            }));

            setBeanResolver((context, beanName) -> null);

            setConstructorResolvers(Arrays.asList(new ConstructorResolver() {
                @Override
                public ConstructorExecutor resolve(EvaluationContext context, String typeName, List<TypeDescriptor> argumentTypes) throws AccessException {
                    return null;
                }
            }));
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

        private ClassLoader getRootClassloader(){
            ClassLoader classLoader = ClassLoader.getSystemClassLoader();
            while(classLoader.getParent() != null){
                classLoader = classLoader.getParent();
            }
            return classLoader;
        }
    }
}
