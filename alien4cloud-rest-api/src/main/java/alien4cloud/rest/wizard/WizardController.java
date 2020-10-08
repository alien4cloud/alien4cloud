package alien4cloud.rest.wizard;

import alien4cloud.exception.NotFoundException;
import alien4cloud.rest.wizard.model.WizardAddon;
import alien4cloud.webconfiguration.WebTemplatingConfiguration;
import alien4cloud.webconfiguration.WizardAddonsScanner;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This controler is in charge of controlling the wizard view. We need to pass it the optional context path to build the base href.
 */
@Slf4j
@Controller
public class WizardController {

    @Value("${server.url_path:/}")
    private String contextPath;

    private String baseHrefValue;

    @Resource
    private RequestMappingHandlerMapping handlerMapping;

    @Resource
    private WizardAddonsScanner wizardAddonsScanner;

    private static ThreadLocal<Pattern> ADDON_CONTEXT_PATH_DETECTION_PATTERN = new ThreadLocal<Pattern>() {
        @Override
        protected Pattern initialValue() {
            return Pattern.compile(".*\\/(.+)\\/?$");
        }
    };

    @PostConstruct
    public void init() {
        this.baseHrefValue = (contextPath.equals("/")) ? "/wizard/" : contextPath + "/wizard/";

        try {
            log.info("Registering wizard addons handler mappings");
            Method method = WizardController.class.getDeclaredMethod("addonIndexHandler", HttpServletRequest.class);
            for (WizardAddon addon : wizardAddonsScanner.getAddons().values()) {
                log.info("Registering wizard addon handler mapping for path {}", addon.getContextPath());
                handlerMapping.registerMapping(
                        RequestMappingInfo.paths("/" + addon.getContextPath(), "/" + addon.getContextPath() + "/#").methods(RequestMethod.GET).build(),
                        this,
                        method);
            }
        } catch(NoSuchMethodException nsme) {
            log.error("Can not define handler mapping method for addons", nsme);
        }
    }

    @RequestMapping(value = { "/wizard", "/wizard/#"}, method = RequestMethod.GET)
    public ModelAndView index() {
        if (log.isDebugEnabled()) {
            log.debug("Base Href value for wizard is: " + baseHrefValue);
        }
        ModelAndView mv = new ModelAndView("wizard", "baseHrefValue", baseHrefValue);
        return mv;
    }

    /**
     * When we reach this handler method that's because we ask for a wizard addon.
     * The addon name is detected from the request URI in order to provide the correct view name.
     * @see  WebTemplatingConfiguration#templateEngine()
     * @param httpServletRequest
     * @return
     */
    public ModelAndView addonIndexHandler(HttpServletRequest httpServletRequest) {
        Matcher m = ADDON_CONTEXT_PATH_DETECTION_PATTERN.get().matcher(httpServletRequest.getPathInfo());
        if (m.matches() && m.group(1) != null && StringUtils.isNotEmpty(m.group(1))) {
            String addonContextName = m.group(1);
            if (addonContextName.endsWith("/")) {
                addonContextName = addonContextName.substring(0, addonContextName.length() - 1);
            }
            if (wizardAddonsScanner.getAddons().containsKey(addonContextName)) {
                String addonPath = "/" + addonContextName + "/";
                String addonsBaseHrefValue = (contextPath.equals("/")) ? addonPath : contextPath + addonPath;
                ModelAndView mv = new ModelAndView(addonContextName, "baseHrefValue", addonsBaseHrefValue);
                return mv;
            } else {
                log.warn("Addon context path not recognized for URI {}", httpServletRequest.getRequestURI());
                throw new NotFoundException("Not able to handle request for URI: " + httpServletRequest.getRequestURI());
            }
        } else {
            throw new NotFoundException("Not able to handle request for URI: " + httpServletRequest.getRequestURI());
        }
    }

}
