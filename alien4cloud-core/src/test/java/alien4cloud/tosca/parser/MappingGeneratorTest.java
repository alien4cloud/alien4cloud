package alien4cloud.tosca.parser;

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import alien4cloud.tosca.parser.mapping.generator.MappingGenerator;

/**
 * Test tosca parsing for Tosca Simple profile in YAML wd03
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:tosca/parser-application-context.xml")
public class MappingGeneratorTest {
    @Resource
    private MappingGenerator mappingGenerator;

    @Test
    public void test() throws ParsingException {
        mappingGenerator.process("tosca-simple-profile-wd03-mapping.yml");
    }
}