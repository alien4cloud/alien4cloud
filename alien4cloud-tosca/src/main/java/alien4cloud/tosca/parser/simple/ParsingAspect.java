package alien4cloud.tosca.parser.simple;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.tosca.parser.ParsingContextExecution;

/**
 * This aspect is used to add every parsed object to the context along with it's original node.
 */
@Aspect
@Component
public class ParsingAspect {
    @Around("execution(* alien4cloud.tosca.parser.INodeParser.parse(..))")
    public Object addToContext(ProceedingJoinPoint joinPoint) throws Throwable {
        Object parsed = joinPoint.proceed();
        if (parsed != null) {
            Node node = (Node) joinPoint.getArgs()[0];
            ParsingContextExecution context = (ParsingContextExecution) joinPoint.getArgs()[1];
            context.getObjectToNodeMap().put(parsed, node);
        }
        return parsed;
    }
}