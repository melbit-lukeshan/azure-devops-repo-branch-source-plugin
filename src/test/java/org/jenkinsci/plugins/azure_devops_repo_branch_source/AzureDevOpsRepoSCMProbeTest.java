package org.jenkinsci.plugins.azure_devops_repo_branch_source;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import jenkins.scm.api.SCMHeadOrigin;
import jenkins.scm.api.mixin.ChangeRequestCheckoutStrategy;
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.AzureConnector;
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model.GitRepositoryWithAzureContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.github.GitHub;

import java.io.FileNotFoundException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AzureDevOpsRepoSCMProbeTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();
    public static WireMockRuleFactory factory = new WireMockRuleFactory();
    @Rule
    public WireMockRule githubApi = factory.getRule(WireMockConfiguration.options().dynamicPort().usingFilesUnderClasspath("api"));
    private AzureDevOpsRepoSCMProbe probe;

    @Before
    public void setUp() throws Exception {
        AzureDevOpsRepoSCMProbe.JENKINS_54126_WORKAROUND = true;
        AzureDevOpsRepoSCMProbe.STAT_RETHROW_API_FNF = true;
        final GitHub github = Connector.connect("http://localhost:" + githubApi.port(), null);
        githubApi.stubFor(
                get(urlEqualTo("/repos/cloudbeers/yolo"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBodyFile("body-cloudbeers-yolo-PucD6.json"))
        );
        //final GHRepository repo = github.getRepository("cloudbeers/yolo");
        final GitRepositoryWithAzureContext repo =
                AzureConnector.INSTANCE.getRepository("http://localhost:" + githubApi.port(), null, "cloudbeers", "yolo");
        final PullRequestSCMHead head = new PullRequestSCMHead("PR-1", "cloudbeers", "yolo", "b", 1, new BranchSCMHead("master", BranchSCMHead.RealBranchType.branch), new SCMHeadOrigin.Fork("rsandell"), ChangeRequestCheckoutStrategy.MERGE);
        probe = new AzureDevOpsRepoSCMProbe(repo,
                head,
                new PullRequestSCMRevision(head, "a", "b", null));
    }

    @Issue("JENKINS-54126")
    @Test(expected = FileNotFoundException.class)
    public void statWhenRootIs404() throws Exception {
        githubApi.stubFor(get(urlPathEqualTo("/repos/cloudbeers/yolo/contents/")).willReturn(aResponse().withStatus(404)));
        probe.stat("Jenkinsfile").exists();
    }

    @Issue("JENKINS-54126")
    @Test
    public void statWhenRootIs404WorkaroundOff() throws Exception {
        AzureDevOpsRepoSCMProbe.JENKINS_54126_WORKAROUND = false;
        AzureDevOpsRepoSCMProbe.STAT_RETHROW_API_FNF = false;
        githubApi.stubFor(get(urlPathEqualTo("/repos/cloudbeers/yolo/contents/")).willReturn(aResponse().withStatus(404)));
        assertFalse(probe.stat("README.md").exists());
    }

    @Issue("JENKINS-54126")
    @Test(expected = FileNotFoundException.class)
    public void statWhenDirAndRootIs404() throws Exception {
        githubApi.stubFor(get(urlPathEqualTo("/repos/cloudbeers/yolo/contents/")).willReturn(aResponse().withStatus(200)));
        githubApi.stubFor(get(urlPathEqualTo("/repos/cloudbeers/yolo/contents/subdir")).willReturn(aResponse().withStatus(404)));
        probe.stat("subdir/Jenkinsfile").exists();
    }

    @Issue("JENKINS-54126")
    @Test
    public void statWhenDirIs404() throws Exception {
        githubApi.stubFor(get(urlPathEqualTo("/repos/cloudbeers/yolo/contents/subdir")).willReturn(aResponse().withStatus(404)));
        githubApi.stubFor(get(urlPathEqualTo("/repos/cloudbeers/yolo/contents/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("body-yolo-contents-8rd37.json")));
        assertFalse(probe.stat("subdir/Jenkinsfile").exists());
    }

    @Issue("JENKINS-54126")
    @Test
    public void statWhenRootIs404AndCacheOnThenOff() throws Exception {
        AzureDevOpsRepoSCMSource.setCacheSize(10);
        githubApi.stubFor(get(urlPathEqualTo("/repos/cloudbeers/yolo/contents/")).withHeader("Cache-Control", containing("max-age")).willReturn(aResponse().withStatus(404)));
        githubApi.stubFor(get(urlPathEqualTo("/repos/cloudbeers/yolo/contents/")).withHeader("Cache-Control", absent())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("body-yolo-contents-8rd37.json")));

        assertTrue(probe.stat("README.md").exists());
        githubApi.verify(RequestPatternBuilder.newRequestPattern(RequestMethod.GET, urlPathEqualTo("/repos/cloudbeers/yolo/contents/")).withHeader("Cache-Control", containing("max-age")));
        githubApi.verify(RequestPatternBuilder.newRequestPattern(RequestMethod.GET, urlPathEqualTo("/repos/cloudbeers/yolo/contents/")).withHeader("Cache-Control", absent()));
    }

}