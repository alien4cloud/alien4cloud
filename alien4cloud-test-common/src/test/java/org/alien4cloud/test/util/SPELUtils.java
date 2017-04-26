package org.alien4cloud.test.util;

import org.junit.Assert;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import com.fasterxml.jackson.core.JsonProcessingException;

import alien4cloud.rest.utils.JsonUtil;

public class SPELUtils {

    public static Object evaluateExpression(EvaluationContext context, String spelExpression) {
        ExpressionParser parser = new SpelExpressionParser();
        Expression exp = parser.parseExpression(spelExpression);
        return exp.getValue(context);
    }

    public static void assertSpelResult(Object expected, Object result, String spelExpression) {
        if ("null".equals(expected)) {
            Assert.assertNull(String.format("The SPEL expression [%s] result should be null", spelExpression), result);
        } else {
            Assert.assertNotNull(String.format("The SPEL expression [%s] result should not be null", spelExpression), result);
            if (result instanceof Long && expected instanceof Integer) {
                expected = ((Integer) expected).longValue();
            }
            Assert.assertEquals(String.format("The SPEL expression [%s] should return [%s]", spelExpression, expected), expected, result);
        }
    }

    public static void evaluateAndAssertExpression(EvaluationContext context, String spelExpression, Object expected) {
        Object result = evaluateExpression(context, spelExpression);
        assertSpelResult(expected, result, spelExpression);
    }

    public static void evaluateAndAssertExpressionContains(EvaluationContext context, String spelExpression, String expectedPart)
            throws JsonProcessingException {
        Object result = evaluateExpression(context, spelExpression);
        Assert.assertTrue(String.format("The SPEL expression [%s] should contains [%s]", spelExpression, expectedPart),
                JsonUtil.toString(result).contains(expectedPart));

    }
}
