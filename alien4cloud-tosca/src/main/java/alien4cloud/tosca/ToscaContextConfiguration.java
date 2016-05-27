package alien4cloud.tosca;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import alien4cloud.tosca.parser.ToscaParser;

/**
 * Context configuration for TOSCA Parser.
 */
@Configuration
@ComponentScan({ "alien4cloud.tosca", "alien4cloud.utils.services", "alien4cloud.paas.wf" })
public class ToscaContextConfiguration {
    @Bean(name = "validator")
    public LocalValidatorFactoryBean validator() {
        return new LocalValidatorFactoryBean();
    }

    private static AnnotationConfigApplicationContext applicationContext = null;

    /**
     * If you are not using SpringFramework in your application, this utility method will load an application and provide you with an instance of the
     * ToscaParser.
     * 
     * @return The instance of the ToscaParser
     */
    public static ToscaParser getParser() {
        if (applicationContext == null) {
            initContext();
        }
        return applicationContext.getBean(ToscaParser.class);
    }

    private static synchronized void initContext() {
        applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.register(ToscaContextConfiguration.class);
        applicationContext.refresh();
        applicationContext.start();
    }
}
