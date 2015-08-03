package alien4cloud.it.provider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import junitx.framework.FileAssert;
import alien4cloud.it.Context;
import alien4cloud.it.provider.util.AttributeUtil;
import alien4cloud.it.provider.util.SSHUtil;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class ScpStepsDefinitions {

    @When("^I upload the local file \"([^\"]*)\" to the node \"([^\"]*)\"'s remote path \"([^\"]*)\" with the keypair \"([^\"]*)\" and user \"([^\"]*)\"$")
    public void I_upload_the_local_file_to_the_node_s_remote_path_with_the_keypair_and_user(String localFile, String nodeName, String remotePath,
            String keypair, String user) throws Throwable {
        SSHUtil.upload(user, AttributeUtil.getAttribute(nodeName, "public_ip_address"), Context.SCP_PORT, Context.LOCAL_TEST_DATA_PATH.resolve(keypair)
                .toString(), remotePath, Context.LOCAL_TEST_DATA_PATH.resolve(localFile).toString());
    }

    @When("^I upload to a node's remote path the local file with the keypair \"([^\"]*)\" and user \"([^\"]*)\"$")
    public void i_upload_to_a_node_s_remote_path_the_local_file_with_the_keypair_and_user(String keypair, String user, List<List<String>> uploadInfos)
            throws Throwable {
        for (List<String> uploadInfo : uploadInfos) {
            String nodeName = uploadInfo.get(0);
            String remotePath = uploadInfo.get(1);
            String localFile = uploadInfo.get(2);
            I_upload_the_local_file_to_the_node_s_remote_path_with_the_keypair_and_user(localFile, nodeName, remotePath, keypair, user);
        }
    }

    private static final String CURRENT_DOWNLOADED_FILE_PATH;

    static {
        try {
            CURRENT_DOWNLOADED_FILE_PATH = Files.createTempFile("provider-int-tst", "").toString();
        } catch (IOException e) {
            throw new RuntimeException("Unable to create temp file");
        }
    }

    @When("^I download the remote file \"([^\"]*)\" from the node \"([^\"]*)\" with the keypair \"([^\"]*)\" and user \"([^\"]*)\"$")
    public void I_download_the_remote_file_from_the_node_with_the_keypair_and_user(String remoteFilePath, String nodeName, String keypair, String user)
            throws Throwable {
        SSHUtil.download(user, AttributeUtil.getAttribute(nodeName, "public_ip_address"), Context.SCP_PORT, Context.LOCAL_TEST_DATA_PATH.resolve(keypair)
                .toString(), remoteFilePath, CURRENT_DOWNLOADED_FILE_PATH);
    }

    @Then("^The downloaded file should have the same content as the local file \"([^\"]*)\"$")
    public void The_downloaded_file_should_have_the_same_content_as_the_local_file(String localFilePath) throws Throwable {
        FileAssert.assertEquals(new File(Context.LOCAL_TEST_DATA_PATH.resolve(localFilePath).toString()), new File(CURRENT_DOWNLOADED_FILE_PATH));
    }
}
