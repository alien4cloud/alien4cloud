package alien4cloud.git;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;

import alien4cloud.utils.FileUtil;

public class RepositoryManagerTest {

    Path localGitPath = Paths.get("target/git_test/local");
    Path remoteGitPath = Paths.get("target/git_test/remote");

    @Before
    public void setUp() throws Exception {
        FileUtil.delete(localGitPath);
        RepositoryManager.create(localGitPath, null);

        FileUtil.delete(remoteGitPath);
/*******************************
ProxySelector.setDefault(new ProxySelector() {
    final ProxySelector delegate = ProxySelector.getDefault();

    @Override
    public List<Proxy> select(URI uri) {
		try {
            return Arrays.asList(new Proxy(Proxy.Type.HTTP, new InetSocketAddress
                    (InetAddress.getByName("193.56.47.20"), 8080)));
		} catch (UnknownHostException e) {
	        return delegate == null ? Arrays.asList(Proxy.NO_PROXY)
                : delegate.select(uri);
		}
    }

    @Override
    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        if (uri == null || sa == null || ioe == null) {
            throw new IllegalArgumentException(
                    "Arguments can't be null.");
        }
    }
});
*********************/
    }

    @Test
    public void isGitRepository() throws Exception {
        assertThat(RepositoryManager.isGitRepository(localGitPath)).isTrue();
    }

    @Test
    public void isGitRepositoryMatchingRemote() throws Exception {
        RepositoryManager.cloneOrCheckout(remoteGitPath, "https://github.com/thockin/test.git", "newbr", "");

        assertThat(RepositoryManager.isGitRepository(remoteGitPath, "https://github.com/thockin/test.git")).isTrue();
        assertThat(RepositoryManager.isGitRepository(remoteGitPath, "https://github.com/thockin/incorrect.git")).isFalse();
    }

    @Test
    public void isOnBranch() throws Exception {
        RepositoryManager.cloneOrCheckout(remoteGitPath, "https://github.com/thockin/test.git", "newbr", "");

        assertThat(RepositoryManager.isOnBranch(remoteGitPath, "newbr")).isTrue();
        assertThat(RepositoryManager.isOnBranch(remoteGitPath, "incorrect")).isFalse();
    }

    @Test
    public void checkoutLocalBranchFromEmptyRepo() throws Exception {
        FileUtil.delete(localGitPath);
        RepositoryManager.create(localGitPath, null);
        RepositoryManager.checkoutExistingBranchOrCreateOrphan(localGitPath, true, null, null, "newBranch");
        assertThat(RepositoryManager.isOnBranch(localGitPath, "newBranch")).isTrue();
    }

    @Test
    public void deleteCurrentBranch() throws Exception {
        FileUtil.delete(localGitPath);
        RepositoryManager.create(localGitPath, null);
        RepositoryManager.checkoutExistingBranchOrCreateOrphan(localGitPath, true, null, null, "newBranch");

        // as the current branch is "newBranch" the deletion will go to tmp branch
        RepositoryManager.deleteBranch(localGitPath, "newBranch", false);

        assertThat(RepositoryManager.isOnBranch(localGitPath, "tmp")).isTrue();
    }

}
