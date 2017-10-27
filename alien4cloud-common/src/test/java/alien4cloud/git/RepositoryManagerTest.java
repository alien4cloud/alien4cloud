package alien4cloud.git;

import static org.assertj.core.api.Assertions.assertThat;

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
        RepositoryManager.checkoutExistingBranchOrCreateOrphan(localGitPath, true,null, null, "newBranch");
        assertThat(RepositoryManager.isOnBranch(localGitPath, "newBranch")).isTrue();
    }

}