package alien4cloud.it.provider;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import alien4cloud.it.Context;
import alien4cloud.it.provider.util.AttributeUtil;
import alien4cloud.it.provider.util.HttpUtil;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Then;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HttpStepsDefinitions {

    @And("^The URL which is defined in attribute \"([^\"]*)\" of the node \"([^\"]*)\" should work$")
    public void The_URL_which_is_defined_in_attribute_of_the_node_should_work(String attributeName, String nodeName) throws Throwable {
        The_URL_which_is_defined_in_attribute_of_the_node_should_work_and_the_html_should_contain(attributeName, nodeName, null);
    }

    @And("^The URL which is defined in attribute \"([^\"]*)\" of the node \"([^\"]*)\" suffixed by \"([^\"]*)\" should work$")
    public void The_URL_which_is_defined_in_attribute_of_the_node_should_work(String attributeName, String nodeName, String suffix) throws Throwable {
        String baseUrl = AttributeUtil.getAttribute(nodeName, attributeName);
        String toCheck = null;
        if (baseUrl != null) {
            if (baseUrl.endsWith("/")) {
                toCheck = baseUrl.concat(suffix);
            } else {
                toCheck = baseUrl.concat("/").concat(suffix);
            }
        }
        HttpUtil.checkUrl(toCheck, null, 2 * 60 * 1000L);
    }

    @And("^The URL which is defined in attribute \"([^\"]*)\" of the node \"([^\"]*)\" should work and the html should contain \"([^\"]*)\"$")
    public void The_URL_which_is_defined_in_attribute_of_the_node_should_work_and_the_html_should_contain(String attributeName, String nodeName,
            String expectedContent) throws Throwable {
        HttpUtil.checkUrl(AttributeUtil.getAttribute(nodeName, attributeName), expectedContent, 2 * 60 * 1000L);
    }

    @And("^The URL which is defined in attribute \"([^\"]*)\" of the node \"([^\"]*)\" of the application \"([^\"]*)\" should work and the html should contain \"([^\"]*)\"$")
    public void The_URL_which_is_defined_in_attribute_of_the_node_of_the_application_should_work_and_the_html_should_contain(String attributeName,
            String nodeName, String applicationName, String expectedContent) throws Throwable {
        HttpUtil.checkUrl(AttributeUtil.getAttribute(nodeName, attributeName, applicationName, "Environment"), expectedContent, 2 * 60 * 1000L);
    }

    @Then("^I store the attribute \"(.*?)\" of the node \"(.*?)\" as registered string \"(.*?)\"$")
    public void i_store_the_attribute_of_the_node_as_registered_string(String attributeName, String nodeName, String key) throws Throwable {
        String attributeValue = AttributeUtil.getAttribute(nodeName, attributeName);
        Context.getInstance().registerStringContent(key, attributeValue);
    }

    @And("^I call the URL which is defined in registered string \"([^\"]*)\" with path \"([^\"]*)\" and fetch the response and store it in the context as \"([^\"]*)\"$")
    public void fetchUrlContentFromRegisteredString(String urlKey, String path, String key) throws Throwable {
        String url = Context.getInstance().getRegisteredStringContent(urlKey);
        String response = HttpUtil.fetchUrl(url + path, 2 * 60 * 1000L);
        Context.getInstance().registerStringContent(key, response);
    }

    @And("^I call the URL which is defined in attribute \"([^\"]*)\" of the node \"([^\"]*)\" with path \"([^\"]*)\" and fetch the response and store it in the context as \"([^\"]*)\"$")
    public void fetchUrlContent(String attributeName, String nodeName, String path, String key) throws Throwable {
        String response = HttpUtil.fetchUrl(AttributeUtil.getAttribute(nodeName, attributeName) + path, 2 * 60 * 1000L);
        Context.getInstance().registerStringContent(key, response);
    }

    @And("^The URL\\(s\\) which are defined in attribute \"([^\"]*)\" of the (\\d+) instance\\(s\\) of the node \"([^\"]*)\" should work and the html should contain \"([^\"]*)\"$")
    public void The_URL_s_which_are_defined_in_attribute_of_the_instance_s_of_the_node_should_work_and_the_html_should_contain(String attributeName,
            int numberOfInstances, String nodeName, String expectedContent) throws Throwable {
        Map<String, String> allAttributes = AttributeUtil.getAttributes(nodeName, attributeName);
        Assert.assertEquals(numberOfInstances, allAttributes.size());
        for (String url : allAttributes.values()) {
            HttpUtil.checkUrl(url, expectedContent, 2 * 60 * 1000L);
        }
    }

    @And("^The URLs with path \"([^\"]*)\" which are defined in attribute \"([^\"]*)\" of the (\\d+) instances of the node \"([^\"]*)\" should work and the html should contain \"([^\"]*)\"$")
    public void theURLsWithPathWhichAreDefinedInAttributeOfTheInstancesOfTheNodeShouldWorkAndTheHtmlShouldContain(String path, String attributeName,
            int numberOfInstances, String nodeName, String expectedContent) throws Throwable {
        Map<String, String> allAttributes = AttributeUtil.getAttributes(nodeName, attributeName);
        Assert.assertEquals(numberOfInstances, allAttributes.size());
        for (String url : allAttributes.values()) {
            HttpUtil.checkUrl(url + path, expectedContent, 2 * 60 * 1000L);
        }
    }

    @And("^The URL which is defined in attribute \"([^\"]*)\" of the node \"([^\"]*)\" should work and the html should contain \"([^\"]*)\" and \"([^\"]*)\"$")
    public void The_URL_which_is_defined_in_attribute_of_the_node_should_work_and_the_html_should_contain_and(String attributeName, String nodeName,
            String expectedContent, String otherExpectedContent) throws Throwable {
        The_URL_which_is_defined_in_attribute_of_the_node_should_work_and_the_html_should_contain(attributeName, nodeName, expectedContent);
        The_URL_which_is_defined_in_attribute_of_the_node_should_work_and_the_html_should_contain(attributeName, nodeName, otherExpectedContent);
    }

    private class CheckUrlWorker implements Runnable {

        private String attributeName;

        private String nodeName;

        private String expectedContent;

        public CheckUrlWorker(String attributeName, String nodeName, String expectedContent) {
            this.attributeName = attributeName;
            this.nodeName = nodeName;
            this.expectedContent = expectedContent;
        }

        @Override
        public void run() {
            try {
                The_URL_which_is_defined_in_attribute_of_the_node_should_work_and_the_html_should_contain(attributeName, nodeName, expectedContent);
            } catch (InterruptedException e) {
                log.info("Check URL for " + attributeName + " of node " + nodeName + "has been interrupted");
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }
    }

    private class CheckUrlCallback implements FutureCallback<Object> {
        private AtomicInteger failureCount = new AtomicInteger(0);

        private AtomicBoolean success = new AtomicBoolean(false);

        @Override
        public synchronized void onSuccess(Object result) {
            this.success.set(true);
            this.notify();
        }

        @Override
        public synchronized void onFailure(Throwable t) {
            log.info("CheckUrlWorker failed", t);
            if (failureCount.incrementAndGet() == 2) {
                this.notify();
            }
        }

        public synchronized boolean isSuccessful() {
            return success.get();
        }
    }

    @And("^The URL which is defined in attribute \"([^\"]*)\" of the node \"([^\"]*)\" should work and the html should contain \"([^\"]*)\" or \"([^\"]*)\"$")
    public void The_URL_which_is_defined_in_attribute_of_the_node_should_work_and_the_html_should_contain_or(final String attributeName, final String nodeName,
            final String expectedContent, final String otherExpectedContent) throws Throwable {
        final ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                t.setName("CheckURLThread");
                return t;
            }
        }));

        ListenableFuture future = executor.submit(new CheckUrlWorker(attributeName, nodeName, expectedContent));
        ListenableFuture otherFuture = executor.submit(new CheckUrlWorker(attributeName, nodeName, otherExpectedContent));
        CheckUrlCallback futureCallback = new CheckUrlCallback();
        Futures.addCallback(future, futureCallback);
        Futures.addCallback(otherFuture, futureCallback);
        synchronized (futureCallback) {
            futureCallback.wait();
        }
        if (!futureCallback.isSuccessful()) {
            throw new RuntimeException("None of the urls to check is valid");
        } else {
            log.info("Test has been successfull");
        }
        executor.shutdownNow();
    }
}
