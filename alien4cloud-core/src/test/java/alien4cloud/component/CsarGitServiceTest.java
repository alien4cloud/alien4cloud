package alien4cloud.component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.net.URI;
import java.util.Arrays;

import javax.annotation.Resource;

import alien4cloud.tosca.parser.impl.ErrorCode;
import org.alien4cloud.tosca.model.Csar;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import alien4cloud.csar.services.CsarGitRepositoryService;
import alien4cloud.csar.services.CsarGitService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.model.git.CsarGitCheckoutLocation;
import alien4cloud.model.git.CsarGitRepository;
import alien4cloud.tosca.parser.ParserTestUtil;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.utils.FileUtil;
import lombok.extern.slf4j.Slf4j;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:application-context-test.xml")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Slf4j
public class CsarGitServiceTest {

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    CsarGitService csarGitService;
    @Resource
    CsarGitRepositoryService csarGitRepositoryService;
    @Value("${directories.alien}/${directories.upload_temp}")
    private String alienRepoDir;

    @Before
    public void cleanup() {
        alienDAO.delete(CsarGitRepository.class, QueryBuilders.matchAllQuery());
        alienDAO.delete(Csar.class, QueryBuilders.matchAllQuery());
        if (Files.isDirectory(Paths.get(alienRepoDir))) {
            log.debug("cleaning the test env");
            try {
                FileUtil.delete(Paths.get(alienRepoDir));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

ProxySelector.setDefault(new ProxySelector() {
    final ProxySelector delegate = ProxySelector.getDefault();

    @Override
    public List<Proxy> select(URI uri) {
/****
            // Filter the URIs to be proxied
        if (uri.toString().contains("github")
                && uri.toString().contains("https")) {
            return Arrays.asList(new Proxy(Proxy.Type.HTTP, InetSocketAddress
                    .createUnresolved("localhost", 3128)));
        }
        if (uri.toString().contains("github")
                && uri.toString().contains("http")) {
            return Arrays.asList(new Proxy(Proxy.Type.HTTP, InetSocketAddress
                    .createUnresolved("localhost", 3129)));
        }
            // revert to the default behaviour
        return delegate == null ? Arrays.asList(Proxy.NO_PROXY)
                : delegate.select(uri);
***/
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

    }

    @Test
    public void importOneBranchFromGit() {
        CsarGitCheckoutLocation alien12Location = new CsarGitCheckoutLocation();
        alien12Location.setBranchId("1.2.0");
        List<CsarGitCheckoutLocation> importLocations = new LinkedList<>();
        importLocations.add(alien12Location);
        String repoId = csarGitRepositoryService.create("https://github.com/alien4cloud/tosca-normative-types.git", "", "", importLocations, false);

        List<ParsingResult<Csar>> result = csarGitService.importFromGitRepository(repoId);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("tosca-normative-types", result.get(0).getResult().getName());
    }

    @Test
    public void importManyBranchFromGit() {
        CsarGitCheckoutLocation alien12Location = new CsarGitCheckoutLocation();
        alien12Location.setBranchId("1.2.0");
        CsarGitCheckoutLocation alien14Location = new CsarGitCheckoutLocation();
        alien14Location.setBranchId("1.4.0");
        List<CsarGitCheckoutLocation> importLocations = new LinkedList<>();
        importLocations.add(alien12Location);
        importLocations.add(alien14Location);
        String repoId = csarGitRepositoryService.create("https://github.com/alien4cloud/tosca-normative-types.git", "", "", importLocations, false);

        List<ParsingResult<Csar>> result = csarGitService.importFromGitRepository(repoId);
        Assert.assertEquals(2, result.size());
        Assert.assertEquals("tosca-normative-types", result.get(0).getResult().getName());
        Assert.assertEquals("1.0.0-ALIEN12", result.get(0).getResult().getVersion());
        Assert.assertEquals("tosca-normative-types", result.get(1).getResult().getName());
        Assert.assertEquals("1.0.0-ALIEN14", result.get(1).getResult().getVersion());
    }

    @Test
    public void importManyBranchFromGitAndStoreLocally() {
        CsarGitCheckoutLocation alien12Location = new CsarGitCheckoutLocation();
        alien12Location.setBranchId("1.2.0");
        CsarGitCheckoutLocation alien14Location = new CsarGitCheckoutLocation();
        alien14Location.setBranchId("1.4.0");
        List<CsarGitCheckoutLocation> importLocations = new LinkedList<>();
        importLocations.add(alien12Location);
        importLocations.add(alien14Location);
        String repoId = csarGitRepositoryService.create("https://github.com/alien4cloud/tosca-normative-types.git", "", "", importLocations, true);

        List<ParsingResult<Csar>> result = csarGitService.importFromGitRepository(repoId);
        Assert.assertEquals(2, result.size());
        Assert.assertEquals("tosca-normative-types", result.get(0).getResult().getName());
        Assert.assertEquals("1.0.0-ALIEN12", result.get(0).getResult().getVersion());
        Assert.assertEquals("tosca-normative-types", result.get(1).getResult().getName());
        Assert.assertEquals("1.0.0-ALIEN14", result.get(1).getResult().getVersion());

        // now we re-import
        result = csarGitService.importFromGitRepository(repoId);
        Assert.assertEquals(2, result.size());
        Assert.assertEquals(ErrorCode.CSAR_ALREADY_INDEXED, result.get(0).getContext().getParsingErrors().get(0).getErrorCode());
        Assert.assertEquals(ErrorCode.CSAR_ALREADY_INDEXED, result.get(1).getContext().getParsingErrors().get(0).getErrorCode());
    }

    @Test
    public void importArchiveInProperOrder() {
        CsarGitCheckoutLocation normativeTypesMasterLocation = new CsarGitCheckoutLocation();
        normativeTypesMasterLocation.setBranchId("1.2.0");
        List<CsarGitCheckoutLocation> importLocations = new LinkedList<>();
        importLocations.add(normativeTypesMasterLocation);
        String repoId = csarGitRepositoryService.create("https://github.com/alien4cloud/tosca-normative-types.git", "", "", importLocations, false);

        List<ParsingResult<Csar>> result = csarGitService.importFromGitRepository(repoId);
        Assert.assertFalse(result.get(0).hasError(ParsingErrorLevel.ERROR));

        CsarGitCheckoutLocation testArchiveLocation = new CsarGitCheckoutLocation();
        testArchiveLocation.setBranchId("tests/test-order-import");
        testArchiveLocation.setSubPath("tests/test-order-import");
        importLocations.clear();
        importLocations.add(testArchiveLocation);
        repoId = csarGitRepositoryService.create("https://github.com/alien4cloud/samples.git", "", "", importLocations, false);
        List<ParsingResult<Csar>> sampleResult = csarGitService.importFromGitRepository(repoId);

        Assert.assertEquals(3, sampleResult.size());

        for (ParsingResult<Csar> csarParsingResult : sampleResult) {
            boolean hasError = csarParsingResult.hasError(ParsingErrorLevel.ERROR);
            if (hasError) {
                ParserTestUtil.displayErrors(csarParsingResult);
            }
            Assert.assertFalse(hasError);
        }

        Assert.assertEquals("test-archive-one", sampleResult.get(0).getResult().getName());
        Assert.assertEquals("test-archive-two", sampleResult.get(1).getResult().getName());
        Assert.assertEquals("test-archive-three", sampleResult.get(2).getResult().getName());

    }

}
