package alien4cloud.rest.wizard;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class WizardIndexController {

    @RequestMapping(value = { "/wizard-index" }, method = RequestMethod.GET)
    public String index(Model model) {

        String base_href = "/toto";
        model.addAttribute("base_href", base_href);

        return "wizard";
    }

}
