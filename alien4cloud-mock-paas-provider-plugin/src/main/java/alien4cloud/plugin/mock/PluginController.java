package alien4cloud.plugin.mock;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sample rest service provided by a plugin.
 */
@RestController
@RequestMapping("/plugins/mockplugin")
public class PluginController {
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String hello() {
        return "hello";
    }
}