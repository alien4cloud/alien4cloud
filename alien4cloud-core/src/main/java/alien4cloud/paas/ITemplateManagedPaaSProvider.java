package alien4cloud.paas;

import alien4cloud.paas.model.PaaSComputeTemplate;

public interface ITemplateManagedPaaSProvider {

    /**
     * Retrieve the list of compute templates. The PaaS provider which manages .
     *
     * @return the list of compute templates
     */
    PaaSComputeTemplate[] getAvailablePaaSComputeTemplates();
}
