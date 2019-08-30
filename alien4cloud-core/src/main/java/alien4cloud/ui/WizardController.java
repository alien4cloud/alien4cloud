package alien4cloud.ui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class WizardController {

@RequestMapping({ "/wizard", "/wizard/#"})
   public String index() {
       return "forward:/wizard/index.html";
   }
}