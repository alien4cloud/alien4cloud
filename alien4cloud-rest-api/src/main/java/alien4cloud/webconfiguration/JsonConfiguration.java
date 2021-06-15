package alien4cloud.webconfiguration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import alien4cloud.rest.utils.RestMapper;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Configuration for json mapping.
 */
@Configuration
public class JsonConfiguration {
    @Bean
    public ObjectMapper builder(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper mapper = new RestMapper();
        mapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
        builder.configure(mapper);
        return mapper;
    }
}
