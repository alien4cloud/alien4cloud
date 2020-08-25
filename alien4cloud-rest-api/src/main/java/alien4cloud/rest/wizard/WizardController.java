package alien4cloud.rest.wizard;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.PostConstruct;

/**
 * This controler is in charge of controlling the wizard view. We need to pass it the optional context path to build the base href.
 */
@Slf4j
@Controller
public class WizardController {

    @Value("${server.url_path:/}")
    private String contextPath;

    private String baseHrefValue;

    @PostConstruct
    public void init() {
        this.baseHrefValue = (contextPath.equals("/")) ? "/wizard/" : contextPath + "/wizard/";
    }

    @RequestMapping(value = { "/wizard", "/wizard/#"}, method = RequestMethod.GET)
    public ModelAndView index(ModelMap model) {
        if (log.isDebugEnabled()) {
            log.debug("Base Href value for wizard is: " + baseHrefValue);
        }
        ModelAndView mv = new ModelAndView("wizard", "baseHrefValue", baseHrefValue);
        return mv;
    }

}
