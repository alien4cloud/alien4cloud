package alien4cloud.rest.wizard;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@Slf4j
public class WizardIndexController {

    @RequestMapping(value = { "/wizard-index" }, method = RequestMethod.GET)
    public String index(Model model) {
        log.info("WizardIndexController was called");
        String base_href = "/toto";
        model.addAttribute("base_href", base_href);

        return "wizard";
    }

}
