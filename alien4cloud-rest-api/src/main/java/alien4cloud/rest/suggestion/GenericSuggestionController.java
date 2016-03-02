package alien4cloud.rest.suggestion;

import java.io.IOException;

import javax.annotation.Resource;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.rest.model.RestResponse;

@RestController
@RequestMapping({"/rest/suggestions", "/rest/v1/suggestions", "/rest/latest/suggestions"})
public class GenericSuggestionController {

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    public RestResponse<String[]> getSuggestionsById(@PathVariable("index") String index, @PathVariable("type") String type, @PathVariable("path") String path,
            @RequestParam("text") String searchText) throws IOException {

    }
}
