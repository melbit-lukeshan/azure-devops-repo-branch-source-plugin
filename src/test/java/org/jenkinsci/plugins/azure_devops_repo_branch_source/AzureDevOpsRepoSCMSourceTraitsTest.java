package org.jenkinsci.plugins.azure_devops_repo_branch_source;

import jenkins.model.Jenkins;
import jenkins.scm.api.mixin.ChangeRequestCheckoutStrategy;
import jenkins.scm.api.trait.SCMSourceTrait;
import jenkins.scm.impl.trait.WildcardSCMHeadFilterTrait;
import org.hamcrest.Matchers;
import org.jenkinsci.plugins.structs.describable.DescribableModel;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class AzureDevOpsRepoSCMSourceTraitsTest {
    /**
     * All tests in this class only use Jenkins for the extensions
     */
    @ClassRule
    public static JenkinsRule r = new JenkinsRule();

    @Rule
    public TestName currentTestName = new TestName();

    private AzureDevOpsRepoSCMSource load() {
        return load(currentTestName.getMethodName());
    }

    private AzureDevOpsRepoSCMSource load(String dataSet) {
        return (AzureDevOpsRepoSCMSource) Jenkins.XSTREAM2.fromXML(
                getClass().getResource(getClass().getSimpleName() + "/" + dataSet + ".xml"));
    }

    @Test
    public void given__configuredInstance__when__uninstantiating__then__deprecatedFieldsIgnored() throws Exception {
        AzureDevOpsRepoSCMSource instance = new AzureDevOpsRepoSCMSource("https://dev.azure.com/lukeshan", "repo", "spring");
        instance.setId("test");
        assertThat(DescribableModel.uninstantiate2_(instance).toString(),
                is("@github(id=test,repoOwner=repo-owner,repository=repo)")
        );
        instance.setBuildOriginBranch(true);
        instance.setBuildOriginBranchWithPR(false);
        instance.setBuildOriginPRHead(false);
        instance.setBuildOriginPRMerge(true);
        instance.setBuildForkPRHead(true);
        instance.setBuildForkPRMerge(false);
        instance.setIncludes("i*");
        instance.setExcludes("production");
        instance.setScanCredentialsId("foo");
        assertThat(DescribableModel.uninstantiate2_(instance).toString(),
                is("@github("
                        + "credentialsId=foo,"
                        + "id=test,"
                        + "repoOwner=repo-owner,"
                        + "repository=repo,"
                        + "traits=["
                        + "@gitHubBranchDiscovery$org.jenkinsci.plugins.azure_devops_repo_branch_source.BranchDiscoveryTrait(strategyId=1), "
                        + "$OriginPullRequestDiscoveryTrait(strategyId=1), "
                        + "@gitHubForkDiscovery$ForkPullRequestDiscoveryTrait("
                        + "strategyId=2,"
                        + "trust=@gitHubTrustPermissions$TrustPermission()), "
                        + "@headWildcardFilter$WildcardSCMHeadFilterTrait(excludes=production,includes=i*)])")
        );
    }

    @Test
    public void modern() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getId(), is("e4d8c11a-0d24-472f-b86b-4b017c160e9a"));
//        assertThat(instance.getApiUri(), is(nullValue()));
//        assertThat(instance.getRepoOwner(), is("cloudbeers"));
        assertThat(instance.getRepository(), is("stunning-adventure"));
        assertThat(instance.getCredentialsId(), is(nullValue()));
        assertThat(instance.getTraits(), is(Collections.<SCMSourceTrait>emptyList()));
        // Legacy API
        assertThat(instance.getCheckoutCredentialsId(), is(AzureDevOpsRepoSCMSource.DescriptorImpl.SAME));
        assertThat(instance.getIncludes(), is("*"));
        assertThat(instance.getExcludes(), is(""));
        assertThat(instance.getBuildOriginBranch(), is(false));
        assertThat(instance.getBuildOriginBranchWithPR(), is(false));
        assertThat(instance.getBuildOriginPRHead(), is(false));
        assertThat(instance.getBuildOriginPRMerge(), is(false));
        assertThat(instance.getBuildForkPRHead(), is(false));
        assertThat(instance.getBuildForkPRMerge(), is(false));
    }

    @Test
    public void basic_cloud() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getId(), is("org.jenkinsci.plugins.azure_devops_repo_branch_source.AzureDevOpsRepoSCMNavigator"
                + "::https://api.AzureDevOpsRepo.com"
                + "::cloudbeers"
                + "::stunning-adventure"));
//        assertThat(instance.getApiUri(), is(nullValue()));
//        assertThat(instance.getRepoOwner(), is("cloudbeers"));
        assertThat(instance.getRepository(), is("stunning-adventure"));
        assertThat(instance.getCredentialsId(), is("e4d8c11a-0d24-472f-b86b-4b017c160e9a"));
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(true)),
                                hasProperty("buildBranchesWithPR", is(true))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(1)),
                                hasProperty("trust", instanceOf(ForkPullRequestDiscoveryTrait.TrustPermission.class))
                        )
                )
        );
        // Legacy API
        assertThat(instance.getCheckoutCredentialsId(), is(AzureDevOpsRepoSCMSource.DescriptorImpl.SAME));
        assertThat(instance.getIncludes(), is("*"));
        assertThat(instance.getExcludes(), is(""));
        assertThat(instance.getBuildOriginBranch(), is(true));
        assertThat(instance.getBuildOriginBranchWithPR(), is(true));
        assertThat(instance.getBuildOriginPRHead(), is(false));
        assertThat(instance.getBuildOriginPRMerge(), is(false));
        assertThat(instance.getBuildForkPRHead(), is(false));
        assertThat(instance.getBuildForkPRMerge(), is(true));
    }

    @Test
    public void basic_server() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getId(), is("org.jenkinsci.plugins.azure_devops_repo_branch_source.AzureDevOpsRepoSCMNavigator"
                + "::https://AzureDevOpsRepo.test/api/v3"
                + "::cloudbeers"
                + "::stunning-adventure"));
//        assertThat(instance.getApiUri(), is("https://azuredevopsrepo.test/api/v3"));
//        assertThat(instance.getRepoOwner(), is("cloudbeers"));
        assertThat(instance.getRepository(), is("stunning-adventure"));
        assertThat(instance.getCredentialsId(), is("e4d8c11a-0d24-472f-b86b-4b017c160e9a"));
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(true)),
                                hasProperty("buildBranchesWithPR", is(false))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(2))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(2)),
                                hasProperty("trust", instanceOf(ForkPullRequestDiscoveryTrait.TrustPermission.class))
                        )
                )
        );
        // Legacy API
        assertThat(instance.getCheckoutCredentialsId(), is(AzureDevOpsRepoSCMSource.DescriptorImpl.SAME));
        assertThat(instance.getIncludes(), is("*"));
        assertThat(instance.getExcludes(), is(""));
        assertThat(instance.getBuildOriginBranch(), is(true));
        assertThat(instance.getBuildOriginBranchWithPR(), is(false));
        assertThat(instance.getBuildOriginPRHead(), is(true));
        assertThat(instance.getBuildOriginPRMerge(), is(false));
        assertThat(instance.getBuildForkPRHead(), is(true));
        assertThat(instance.getBuildForkPRMerge(), is(false));
    }

    @Test
    public void custom_checkout_credentials() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getId(), is("org.jenkinsci.plugins.azure_devops_repo_branch_source.AzureDevOpsRepoSCMNavigator"
                + "::https://AzureDevOpsRepo.test/api/v3"
                + "::cloudbeers"
                + "::stunning-adventure"));
//        assertThat(instance.getApiUri(), is("https://azuredevopsrepo.test/api/v3"));
//        assertThat(instance.getRepoOwner(), is("cloudbeers"));
        assertThat(instance.getRepository(), is("stunning-adventure"));
        assertThat(instance.getCredentialsId(), is("e4d8c11a-0d24-472f-b86b-4b017c160e9a"));
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(true)),
                                hasProperty("buildBranchesWithPR", is(false))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(2))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(2)),
                                hasProperty("trust", instanceOf(ForkPullRequestDiscoveryTrait.TrustPermission.class))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                Matchers.instanceOf(SSHCheckoutTrait.class),
                                hasProperty("credentialsId", is("other-credentials"))
                        )
                )
        );
        // Legacy API
        assertThat(instance.getCheckoutCredentialsId(), is("other-credentials"));
        assertThat(instance.getIncludes(), is("*"));
        assertThat(instance.getExcludes(), is(""));
        assertThat(instance.getBuildOriginBranch(), is(true));
        assertThat(instance.getBuildOriginBranchWithPR(), is(false));
        assertThat(instance.getBuildOriginPRHead(), is(true));
        assertThat(instance.getBuildOriginPRMerge(), is(false));
        assertThat(instance.getBuildForkPRHead(), is(true));
        assertThat(instance.getBuildForkPRMerge(), is(false));
    }

    @Issue("JENKINS-45467")
    @Test
    public void same_checkout_credentials() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getId(), is("org.jenkinsci.plugins.azure_devops_repo_branch_source.AzureDevOpsRepoSCMNavigator"
                + "::https://AzureDevOpsRepo.test/api/v3"
                + "::cloudbeers"
                + "::stunning-adventure"));
//        assertThat(instance.getApiUri(), is("https://azuredevopsrepo.test/api/v3"));
//        assertThat(instance.getRepoOwner(), is("cloudbeers"));
        assertThat(instance.getRepository(), is("stunning-adventure"));
        assertThat(instance.getCredentialsId(), is("e4d8c11a-0d24-472f-b86b-4b017c160e9a"));
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(true)),
                                hasProperty("buildBranchesWithPR", is(false))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(2))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(2)),
                                hasProperty("trust", instanceOf(ForkPullRequestDiscoveryTrait.TrustPermission.class))
                        )
                )
        );
        // Legacy API
        assertThat(instance.getCheckoutCredentialsId(), is(AzureDevOpsRepoSCMSource.DescriptorImpl.SAME));
        assertThat(instance.getIncludes(), is("*"));
        assertThat(instance.getExcludes(), is(""));
        assertThat(instance.getBuildOriginBranch(), is(true));
        assertThat(instance.getBuildOriginBranchWithPR(), is(false));
        assertThat(instance.getBuildOriginPRHead(), is(true));
        assertThat(instance.getBuildOriginPRMerge(), is(false));
        assertThat(instance.getBuildForkPRHead(), is(true));
        assertThat(instance.getBuildForkPRMerge(), is(false));
    }

    @Test
    public void exclude_branches() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getId(), is("org.jenkinsci.plugins.azure_devops_repo_branch_source.AzureDevOpsRepoSCMNavigator"
                + "::https://api.AzureDevOpsRepo.com"
                + "::cloudbeers"
                + "::stunning-adventure"));
//        assertThat(instance.getApiUri(), is(nullValue()));
//        assertThat(instance.getRepoOwner(), is("cloudbeers"));
        assertThat(instance.getRepository(), is("stunning-adventure"));
        assertThat(instance.getCredentialsId(), is("e4d8c11a-0d24-472f-b86b-4b017c160e9a"));
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(true)),
                                hasProperty("buildBranchesWithPR", is(true))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(1)),
                                hasProperty("trust", instanceOf(ForkPullRequestDiscoveryTrait.TrustPermission.class))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(WildcardSCMHeadFilterTrait.class),
                                hasProperty("includes", is("*")),
                                hasProperty("excludes", is("master"))
                        )
                )
        );
        // Legacy API
        assertThat(instance.getCheckoutCredentialsId(), is(AzureDevOpsRepoSCMSource.DescriptorImpl.SAME));
        assertThat(instance.getIncludes(), is("*"));
        assertThat(instance.getExcludes(), is("master"));
        assertThat(instance.getBuildOriginBranch(), is(true));
        assertThat(instance.getBuildOriginBranchWithPR(), is(true));
        assertThat(instance.getBuildOriginPRHead(), is(false));
        assertThat(instance.getBuildOriginPRMerge(), is(false));
        assertThat(instance.getBuildForkPRHead(), is(false));
        assertThat(instance.getBuildForkPRMerge(), is(true));
    }

    @Test
    public void limit_branches() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getId(), is("org.jenkinsci.plugins.azure_devops_repo_branch_source.AzureDevOpsRepoSCMNavigator"
                + "::https://api.AzureDevOpsRepo.com"
                + "::cloudbeers"
                + "::stunning-adventure"));
//        assertThat(instance.getApiUri(), is(nullValue()));
//        assertThat(instance.getRepoOwner(), is("cloudbeers"));
        assertThat(instance.getRepository(), is("stunning-adventure"));
        assertThat(instance.getCredentialsId(), is("e4d8c11a-0d24-472f-b86b-4b017c160e9a"));
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(true)),
                                hasProperty("buildBranchesWithPR", is(true))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(1)),
                                hasProperty("trust", instanceOf(ForkPullRequestDiscoveryTrait.TrustPermission.class))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(WildcardSCMHeadFilterTrait.class),
                                hasProperty("includes", is("feature/*")),
                                hasProperty("excludes", is(""))
                        )
                )
        );
        // Legacy API
        assertThat(instance.getCheckoutCredentialsId(), is(AzureDevOpsRepoSCMSource.DescriptorImpl.SAME));
        assertThat(instance.getIncludes(), is("feature/*"));
        assertThat(instance.getExcludes(), is(""));
        assertThat(instance.getBuildOriginBranch(), is(true));
        assertThat(instance.getBuildOriginBranchWithPR(), is(true));
        assertThat(instance.getBuildOriginPRHead(), is(false));
        assertThat(instance.getBuildOriginPRMerge(), is(false));
        assertThat(instance.getBuildForkPRHead(), is(false));
        assertThat(instance.getBuildForkPRMerge(), is(true));
    }

    @Test
    public void use_agent_checkout() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getId(), is("org.jenkinsci.plugins.azure_devops_repo_branch_source.AzureDevOpsRepoSCMNavigator"
                + "::https://api.AzureDevOpsRepo.com"
                + "::cloudbeers"
                + "::stunning-adventure"));
//        assertThat(instance.getApiUri(), is(nullValue()));
//        assertThat(instance.getRepoOwner(), is("cloudbeers"));
        assertThat(instance.getRepository(), is("stunning-adventure"));
        assertThat(instance.getCredentialsId(), is("e4d8c11a-0d24-472f-b86b-4b017c160e9a"));
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(true)),
                                hasProperty("buildBranchesWithPR", is(true))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(1)),
                                hasProperty("trust", instanceOf(ForkPullRequestDiscoveryTrait.TrustPermission.class))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                Matchers.instanceOf(SSHCheckoutTrait.class),
                                hasProperty("credentialsId", is(nullValue()))
                        )
                )
        );
        // Legacy API
        assertThat(instance.getCheckoutCredentialsId(), is(AzureDevOpsRepoSCMSource.DescriptorImpl.ANONYMOUS));
        assertThat(instance.getIncludes(), is("*"));
        assertThat(instance.getExcludes(), is(""));
        assertThat(instance.getBuildOriginBranch(), is(true));
        assertThat(instance.getBuildOriginBranchWithPR(), is(true));
        assertThat(instance.getBuildOriginPRHead(), is(false));
        assertThat(instance.getBuildOriginPRMerge(), is(false));
        assertThat(instance.getBuildForkPRHead(), is(false));
        assertThat(instance.getBuildForkPRMerge(), is(true));
    }

    @Test
    public void given__legacyCode__when__constructor_cloud__then__discoveryTraitDefaults() throws Exception {
        AzureDevOpsRepoSCMSource instance = new AzureDevOpsRepoSCMSource("preserve-id", "SAME",
                "e4d8c11a-0d24-472f-b86b-4b017c160e9a", "https://dev.azure.com/lukeshan", "cloudbeers", "spring");
        assertThat(instance.getId(), is("preserve-id"));
//        assertThat(instance.getApiUri(), is(nullValue()));
//        assertThat(instance.getRepoOwner(), is("cloudbeers"));
        assertThat(instance.getRepository(), is("stunning-adventure"));
        assertThat(instance.getCredentialsId(), is("e4d8c11a-0d24-472f-b86b-4b017c160e9a"));
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(true)),
                                hasProperty("buildBranchesWithPR", is(true))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategies", is(EnumSet.of(ChangeRequestCheckoutStrategy.MERGE))),
                                hasProperty("trust", instanceOf(ForkPullRequestDiscoveryTrait.TrustPermission.class))
                        )
                )
        );
        // Legacy API
        assertThat(instance.getCheckoutCredentialsId(), is(AzureDevOpsRepoSCMSource.DescriptorImpl.SAME));
        assertThat(instance.getIncludes(), is("*"));
        assertThat(instance.getExcludes(), is(""));
        assertThat(instance.getBuildOriginBranch(), is(true));
        assertThat(instance.getBuildOriginBranchWithPR(), is(true));
        assertThat(instance.getBuildOriginPRHead(), is(false));
        assertThat(instance.getBuildOriginPRMerge(), is(false));
        assertThat(instance.getBuildForkPRHead(), is(false));
        assertThat(instance.getBuildForkPRMerge(), is(true));
    }

    @Test
    public void given__legacyCode__when__constructor_server__then__discoveryTraitDefaults() throws Exception {
        AzureDevOpsRepoSCMSource instance = new AzureDevOpsRepoSCMSource(null,
                "8b2e4f77-39c5-41a9-b63b-8d367350bfdf", "e4d8c11a-0d24-472f-b86b-4b017c160e9a",
                "https://dev.azure.com/lukeshan", "cloudbeers", "spring");
        assertThat(instance.getId(), is(notNullValue()));
//        assertThat(instance.getApiUri(), is("https://azuredevopsrepo.test/api/v3"));
//        assertThat(instance.getRepoOwner(), is("cloudbeers"));
        assertThat(instance.getRepository(), is("stunning-adventure"));
        assertThat(instance.getCredentialsId(), is("e4d8c11a-0d24-472f-b86b-4b017c160e9a"));
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(true)),
                                hasProperty("buildBranchesWithPR", is(true))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategies", is(EnumSet.of(ChangeRequestCheckoutStrategy.MERGE))),
                                hasProperty("trust", instanceOf(ForkPullRequestDiscoveryTrait.TrustPermission.class))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                Matchers.instanceOf(SSHCheckoutTrait.class),
                                hasProperty("credentialsId", is("8b2e4f77-39c5-41a9-b63b-8d367350bfdf"))
                        )
                )
        );
        // Legacy API
        assertThat(instance.getCheckoutCredentialsId(), is("8b2e4f77-39c5-41a9-b63b-8d367350bfdf"));
        assertThat(instance.getIncludes(), is("*"));
        assertThat(instance.getExcludes(), is(""));
        assertThat(instance.getBuildOriginBranch(), is(true));
        assertThat(instance.getBuildOriginBranchWithPR(), is(true));
        assertThat(instance.getBuildOriginPRHead(), is(false));
        assertThat(instance.getBuildOriginPRMerge(), is(false));
        assertThat(instance.getBuildForkPRHead(), is(false));
        assertThat(instance.getBuildForkPRMerge(), is(true));
    }

    @Test
    public void given__instance__when__setTraits_empty__then__traitsEmpty() {
        AzureDevOpsRepoSCMSource instance = new AzureDevOpsRepoSCMSource("https://dev.azure.com/lukeshan", "test-repo", "spring");
        instance.setTraits(Collections.<SCMSourceTrait>emptyList());
        assertThat(instance.getTraits(), is(Collections.<SCMSourceTrait>emptyList()));
    }

    @Test
    public void given__legacyCode__when__setBuildOriginBranch__then__traitsMaintained() {
        AzureDevOpsRepoSCMSource instance = new AzureDevOpsRepoSCMSource("https://dev.azure.com/lukeshan", "test-repo", "spring");
        instance.setTraits(Collections.<SCMSourceTrait>emptyList());
        assertThat(instance.getTraits(), is(Collections.<SCMSourceTrait>emptyList()));
        instance.setBuildOriginBranch(true);
        assertThat(instance.getTraits(), contains(instanceOf(BranchDiscoveryTrait.class)));
        instance.setBuildOriginBranch(false);
        assertThat(instance.getTraits(), is(Collections.<SCMSourceTrait>emptyList()));

        instance.setBuildOriginBranchWithPR(true);
        assertThat(instance.getTraits(), contains(instanceOf(BranchDiscoveryTrait.class)));
        instance.setBuildOriginBranchWithPR(false);
        assertThat(instance.getTraits(), is(Collections.<SCMSourceTrait>emptyList()));

        instance.setBuildOriginBranchWithPR(true);
        instance.setBuildOriginBranch(true);
        assertThat(instance.getTraits(), contains(instanceOf(BranchDiscoveryTrait.class)));
        instance.setBuildOriginBranchWithPR(false);
        assertThat(instance.getTraits(), contains(instanceOf(BranchDiscoveryTrait.class)));
        instance.setBuildOriginBranch(false);
        assertThat(instance.getTraits(), is(Collections.<SCMSourceTrait>emptyList()));

        instance.setBuildOriginBranchWithPR(true);
        instance.setBuildOriginBranch(true);
        assertThat(instance.getTraits(), contains(instanceOf(BranchDiscoveryTrait.class)));
        instance.setBuildOriginBranch(false);
        assertThat(instance.getTraits(), contains(instanceOf(BranchDiscoveryTrait.class)));
        instance.setBuildOriginBranchWithPR(false);
        assertThat(instance.getTraits(), is(Collections.<SCMSourceTrait>emptyList()));
    }

    @Test
    public void given__instance__when__setTraits__then__traitsSet() {
        AzureDevOpsRepoSCMSource instance = new AzureDevOpsRepoSCMSource("https://dev.azure.com/lukeshan", "test-repo", "spring");
        instance.setTraits(Arrays.asList(new BranchDiscoveryTrait(1),
                new SSHCheckoutTrait("value")));
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(true)),
                                hasProperty("buildBranchesWithPR", is(false))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(SSHCheckoutTrait.class),
                                hasProperty("credentialsId", is("value"))
                        )
                )
        );
    }

    @Test
    public void given__instance__when__setCredentials_empty__then__credentials_null() {
        AzureDevOpsRepoSCMSource instance = new AzureDevOpsRepoSCMSource("https://dev.azure.com/lukeshan", "test-repo", "spring");
        instance.setCredentialsId("");
        assertThat(instance.getCredentialsId(), is(nullValue()));
    }

    @Test
    public void given__instance__when__setCredentials_null__then__credentials_null() {
        AzureDevOpsRepoSCMSource instance = new AzureDevOpsRepoSCMSource("https://dev.azure.com/lukeshan", "test-repo", "spring");
        instance.setCredentialsId("");
        assertThat(instance.getCredentialsId(), is(nullValue()));
    }

    @Test
    public void given__instance__when__setCredentials__then__credentials_set() {
        AzureDevOpsRepoSCMSource instance = new AzureDevOpsRepoSCMSource("https://dev.azure.com/lukeshan", "test-repo", "spring");
        instance.setCredentialsId("test");
        assertThat(instance.getCredentialsId(), is("test"));
    }

    @Test
    public void given__legacyCode_withoutExcludes__when__setIncludes_default__then__traitRemoved() {
        AzureDevOpsRepoSCMSource instance = new AzureDevOpsRepoSCMSource("https://dev.azure.com/lukeshan", "test-repo", "spring");
        instance.setTraits(Arrays.asList(
                new BranchDiscoveryTrait(true, false),
                new WildcardSCMHeadFilterTrait("feature/*", "")
        ));
        assertThat(instance.getIncludes(), is("feature/*"));
        assertThat(instance.getExcludes(), is(""));
        assertThat(instance.getTraits(), Matchers.<SCMSourceTrait>hasItem(allOf(
                instanceOf(WildcardSCMHeadFilterTrait.class),
                hasProperty("includes", is("feature/*")),
                hasProperty("excludes", is(""))
        )));
        instance.setIncludes("*");
        assertThat(instance.getIncludes(), is("*"));
        assertThat(instance.getExcludes(), is(""));
        assertThat(instance.getTraits(), not(Matchers.<SCMSourceTrait>hasItem(
                instanceOf(WildcardSCMHeadFilterTrait.class)
        )));
    }

    @Test
    public void given__legacyCode_withoutExcludes__when__setIncludes_value__then__traitUpdated() {
        AzureDevOpsRepoSCMSource instance = new AzureDevOpsRepoSCMSource("https://dev.azure.com/lukeshan", "test-repo", "spring");
        instance.setTraits(Arrays.asList(
                new BranchDiscoveryTrait(true, false),
                new WildcardSCMHeadFilterTrait("feature/*", "")
        ));
        assertThat(instance.getIncludes(), is("feature/*"));
        assertThat(instance.getExcludes(), is(""));
        assertThat(instance.getTraits(), Matchers.<SCMSourceTrait>hasItem(allOf(
                instanceOf(WildcardSCMHeadFilterTrait.class),
                hasProperty("includes", is("feature/*")),
                hasProperty("excludes", is(""))
        )));
        instance.setIncludes("bug/*");
        assertThat(instance.getIncludes(), is("bug/*"));
        assertThat(instance.getExcludes(), is(""));
        assertThat(instance.getTraits(), Matchers.<SCMSourceTrait>hasItem(allOf(
                instanceOf(WildcardSCMHeadFilterTrait.class),
                hasProperty("includes", is("bug/*")),
                hasProperty("excludes", is(""))
        )));
    }

    @Test
    public void given__legacyCode_withoutTrait__when__setIncludes_value__then__traitAdded() {
        AzureDevOpsRepoSCMSource instance = new AzureDevOpsRepoSCMSource("https://dev.azure.com/lukeshan", "test-repo", "spring");
        instance.setTraits(Arrays.asList(
                new BranchDiscoveryTrait(true, false),
                new SSHCheckoutTrait("someValue")
        ));
        assertThat(instance.getIncludes(), is("*"));
        assertThat(instance.getExcludes(), is(""));
        assertThat(instance.getTraits(), not(Matchers.<SCMSourceTrait>hasItem(
                instanceOf(WildcardSCMHeadFilterTrait.class)
        )));
        instance.setIncludes("feature/*");
        assertThat(instance.getIncludes(), is("feature/*"));
        assertThat(instance.getExcludes(), is(""));
        assertThat(instance.getTraits(), Matchers.<SCMSourceTrait>hasItem(allOf(
                instanceOf(WildcardSCMHeadFilterTrait.class),
                hasProperty("includes", is("feature/*")),
                hasProperty("excludes", is(""))
        )));
    }

    @Test
    public void given__legacyCode_withExcludes__when__setIncludes_default__then__traitUpdated() {
        AzureDevOpsRepoSCMSource instance = new AzureDevOpsRepoSCMSource("https://dev.azure.com/lukeshan", "test-repo", "spring");
        instance.setTraits(Arrays.asList(
                new BranchDiscoveryTrait(true, false),
                new WildcardSCMHeadFilterTrait("feature/*", "feature/ignore"),
                new SSHCheckoutTrait("someValue")
        ));
        assertThat(instance.getIncludes(), is("feature/*"));
        assertThat(instance.getExcludes(), is("feature/ignore"));
        assertThat(instance.getTraits(), Matchers.<SCMSourceTrait>hasItem(allOf(
                instanceOf(WildcardSCMHeadFilterTrait.class),
                hasProperty("includes", is("feature/*")),
                hasProperty("excludes", is("feature/ignore"))
        )));
        instance.setIncludes("*");
        assertThat(instance.getIncludes(), is("*"));
        assertThat(instance.getExcludes(), is("feature/ignore"));
        assertThat(instance.getTraits(), Matchers.<SCMSourceTrait>hasItem(allOf(
                instanceOf(WildcardSCMHeadFilterTrait.class),
                hasProperty("includes", is("*")),
                hasProperty("excludes", is("feature/ignore"))
        )));
    }

    @Test
    public void given__legacyCode_withExcludes__when__setIncludes_value__then__traitUpdated() {
        AzureDevOpsRepoSCMSource instance = new AzureDevOpsRepoSCMSource("https://dev.azure.com/lukeshan", "test-repo", "spring");
        instance.setTraits(Arrays.asList(
                new BranchDiscoveryTrait(true, false),
                new WildcardSCMHeadFilterTrait("feature/*", "feature/ignore"),
                new SSHCheckoutTrait("someValue")
        ));
        assertThat(instance.getIncludes(), is("feature/*"));
        assertThat(instance.getExcludes(), is("feature/ignore"));
        assertThat(instance.getTraits(), Matchers.<SCMSourceTrait>hasItem(allOf(
                instanceOf(WildcardSCMHeadFilterTrait.class),
                hasProperty("includes", is("feature/*")),
                hasProperty("excludes", is("feature/ignore"))
        )));
        instance.setIncludes("bug/*");
        assertThat(instance.getIncludes(), is("bug/*"));
        assertThat(instance.getExcludes(), is("feature/ignore"));
        assertThat(instance.getTraits(), Matchers.<SCMSourceTrait>hasItem(allOf(
                instanceOf(WildcardSCMHeadFilterTrait.class),
                hasProperty("includes", is("bug/*")),
                hasProperty("excludes", is("feature/ignore"))
        )));
    }

    @Test
    public void given__legacyCode_withoutIncludes__when__setExcludes_default__then__traitRemoved() {
        AzureDevOpsRepoSCMSource instance = new AzureDevOpsRepoSCMSource("https://dev.azure.com/lukeshan", "test-repo", "spring");
        instance.setTraits(Arrays.asList(
                new BranchDiscoveryTrait(true, false),
                new WildcardSCMHeadFilterTrait("*", "feature/ignore"),
                new SSHCheckoutTrait("someValue")
        ));
        assertThat(instance.getIncludes(), is("*"));
        assertThat(instance.getExcludes(), is("feature/ignore"));
        assertThat(instance.getTraits(), Matchers.<SCMSourceTrait>hasItem(allOf(
                instanceOf(WildcardSCMHeadFilterTrait.class),
                hasProperty("includes", is("*")),
                hasProperty("excludes", is("feature/ignore"))
        )));
        instance.setExcludes("");
        assertThat(instance.getIncludes(), is("*"));
        assertThat(instance.getExcludes(), is(""));
        assertThat(instance.getTraits(), not(Matchers.<SCMSourceTrait>hasItem(
                instanceOf(WildcardSCMHeadFilterTrait.class)
        )));
    }

    @Test
    public void given__legacyCode_withoutIncludes__when__setExcludes_value__then__traitUpdated() {
        AzureDevOpsRepoSCMSource instance = new AzureDevOpsRepoSCMSource("https://dev.azure.com/lukeshan", "test-repo", "spring");
        instance.setTraits(Arrays.asList(
                new BranchDiscoveryTrait(true, false),
                new WildcardSCMHeadFilterTrait("*", "feature/ignore"),
                new SSHCheckoutTrait("someValue")
        ));
        assertThat(instance.getIncludes(), is("*"));
        assertThat(instance.getExcludes(), is("feature/ignore"));
        assertThat(instance.getTraits(), Matchers.<SCMSourceTrait>hasItem(allOf(
                instanceOf(WildcardSCMHeadFilterTrait.class),
                hasProperty("includes", is("*")),
                hasProperty("excludes", is("feature/ignore"))
        )));
        instance.setExcludes("bug/ignore");
        assertThat(instance.getIncludes(), is("*"));
        assertThat(instance.getExcludes(), is("bug/ignore"));
        assertThat(instance.getTraits(), Matchers.<SCMSourceTrait>hasItem(allOf(
                instanceOf(WildcardSCMHeadFilterTrait.class),
                hasProperty("includes", is("*")),
                hasProperty("excludes", is("bug/ignore"))
        )));
    }

    @Test
    public void given__legacyCode_withoutTrait__when__setExcludes_value__then__traitAdded() {
        AzureDevOpsRepoSCMSource instance = new AzureDevOpsRepoSCMSource("https://dev.azure.com/lukeshan", "test-repo", "spring");
        instance.setTraits(Arrays.asList(
                new BranchDiscoveryTrait(true, false),
                new SSHCheckoutTrait("someValue")
        ));
        assertThat(instance.getIncludes(), is("*"));
        assertThat(instance.getExcludes(), is(""));
        assertThat(instance.getTraits(), not(Matchers.<SCMSourceTrait>hasItem(
                instanceOf(WildcardSCMHeadFilterTrait.class)
        )));
        instance.setExcludes("feature/ignore");
        assertThat(instance.getIncludes(), is("*"));
        assertThat(instance.getExcludes(), is("feature/ignore"));
        assertThat(instance.getTraits(), Matchers.<SCMSourceTrait>hasItem(allOf(
                instanceOf(WildcardSCMHeadFilterTrait.class),
                hasProperty("includes", is("*")),
                hasProperty("excludes", is("feature/ignore"))
        )));
    }

    @Test
    public void given__legacyCode_withIncludes__when__setExcludes_default__then__traitUpdated() {
        AzureDevOpsRepoSCMSource instance = new AzureDevOpsRepoSCMSource("https://dev.azure.com/lukeshan", "test-repo", "spring");
        instance.setTraits(Arrays.asList(
                new BranchDiscoveryTrait(true, false),
                new WildcardSCMHeadFilterTrait("feature/*", "feature/ignore"),
                new SSHCheckoutTrait("someValue")
        ));
        assertThat(instance.getIncludes(), is("feature/*"));
        assertThat(instance.getExcludes(), is("feature/ignore"));
        assertThat(instance.getTraits(), Matchers.<SCMSourceTrait>hasItem(allOf(
                instanceOf(WildcardSCMHeadFilterTrait.class),
                hasProperty("includes", is("feature/*")),
                hasProperty("excludes", is("feature/ignore"))
        )));
        instance.setExcludes("");
        assertThat(instance.getIncludes(), is("feature/*"));
        assertThat(instance.getExcludes(), is(""));
        assertThat(instance.getTraits(), Matchers.<SCMSourceTrait>hasItem(allOf(
                instanceOf(WildcardSCMHeadFilterTrait.class),
                hasProperty("includes", is("feature/*")),
                hasProperty("excludes", is(""))
        )));
    }

    @Test
    public void given__legacyCode_withIncludes__when__setExcludes_value__then__traitUpdated() {
        AzureDevOpsRepoSCMSource instance = new AzureDevOpsRepoSCMSource("https://dev.azure.com/lukeshan", "test-repo", "spring");
        instance.setTraits(Arrays.asList(
                new BranchDiscoveryTrait(true, false),
                new WildcardSCMHeadFilterTrait("feature/*", ""),
                new SSHCheckoutTrait("someValue")
        ));
        assertThat(instance.getIncludes(), is("feature/*"));
        assertThat(instance.getExcludes(), is(""));
        assertThat(instance.getTraits(), Matchers.<SCMSourceTrait>hasItem(allOf(
                instanceOf(WildcardSCMHeadFilterTrait.class),
                hasProperty("includes", is("feature/*")),
                hasProperty("excludes", is(""))
        )));
        instance.setExcludes("feature/ignore");
        assertThat(instance.getIncludes(), is("feature/*"));
        assertThat(instance.getExcludes(), is("feature/ignore"));
        assertThat(instance.getTraits(), Matchers.<SCMSourceTrait>hasItem(allOf(
                instanceOf(WildcardSCMHeadFilterTrait.class),
                hasProperty("includes", is("feature/*")),
                hasProperty("excludes", is("feature/ignore"))
        )));
    }

    @Test
    public void build_000000() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                empty()
        );
    }

    @Test
    public void build_000001() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                contains(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(2))
                        )
                )
        );
    }

    @Test
    public void build_000010() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                contains(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(1))
                        )
                )
        );
    }

    @Test
    public void build_000011() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                contains(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(3))
                        )
                )
        );
    }

    @Test
    public void build_000100() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                contains(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(2))
                        )
                )
        );
    }

    @Test
    public void build_000101() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(2))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(2))
                        )
                )
        );
    }

    @Test
    public void build_000110() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(2))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(1))
                        )
                )
        );
    }

    @Test
    public void build_000111() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(2))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(3))
                        )
                )
        );
    }

    @Test
    public void build_001000() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                contains(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(1))
                        )
                )
        );
    }

    @Test
    public void build_001001() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(1))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(2))
                        )
                )
        );
    }

    @Test
    public void build_001010() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(1))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(1))
                        )
                )
        );
    }

    @Test
    public void build_001011() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(1))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(3))
                        )
                )
        );
    }

    @Test
    public void build_001100() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                contains(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(3))
                        )
                )
        );
    }

    @Test
    public void build_001101() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(3))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(2))
                        )
                )
        );
    }

    @Test
    public void build_001110() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(3))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(1))
                        )
                )
        );
    }

    @Test
    public void build_001111() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(3))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(3))
                        )
                )
        );
    }

    @Test
    public void build_010000() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                contains(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(false)),
                                hasProperty("buildBranchesWithPR", is(true))
                        )
                )
        );
    }

    @Test
    public void build_010001() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(false)),
                                hasProperty("buildBranchesWithPR", is(true))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(2))
                        )
                )
        );
    }

    @Test
    public void build_010010() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(false)),
                                hasProperty("buildBranchesWithPR", is(true))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(1))
                        )
                )
        );
    }

    @Test
    public void build_010011() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(false)),
                                hasProperty("buildBranchesWithPR", is(true))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(3))
                        )
                )
        );
    }

    @Test
    public void build_010100() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(false)),
                                hasProperty("buildBranchesWithPR", is(true))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(2))
                        )
                )
        );
    }

    @Test
    public void build_010101() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(false)),
                                hasProperty("buildBranchesWithPR", is(true))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(2))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(2))
                        )
                )
        );
    }

    @Test
    public void build_010110() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(false)),
                                hasProperty("buildBranchesWithPR", is(true))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(2))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(1))
                        )
                )
        );
    }

    @Test
    public void build_010111() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(false)),
                                hasProperty("buildBranchesWithPR", is(true))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(2))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(3))
                        )
                )
        );
    }

    @Test
    public void build_011000() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(false)),
                                hasProperty("buildBranchesWithPR", is(true))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(1))
                        )
                )
        );
    }

    @Test
    public void build_011001() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(false)),
                                hasProperty("buildBranchesWithPR", is(true))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(1))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(2))
                        )
                )
        );
    }

    @Test
    public void build_011010() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(false)),
                                hasProperty("buildBranchesWithPR", is(true))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(1))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(1))
                        )
                )
        );
    }

    @Test
    public void build_011011() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(false)),
                                hasProperty("buildBranchesWithPR", is(true))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(1))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(3))
                        )
                )
        );
    }

    @Test
    public void build_011100() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(false)),
                                hasProperty("buildBranchesWithPR", is(true))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(3))
                        )
                )
        );
    }

    @Test
    public void build_011101() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(false)),
                                hasProperty("buildBranchesWithPR", is(true))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(3))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(2))
                        )
                )
        );
    }

    @Test
    public void build_011110() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(false)),
                                hasProperty("buildBranchesWithPR", is(true))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(3))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(1))
                        )
                )
        );
    }

    @Test
    public void build_011111() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(false)),
                                hasProperty("buildBranchesWithPR", is(true))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(3))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(3))
                        )
                )
        );
    }

    @Test
    public void build_100000() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                contains(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(true)),
                                hasProperty("buildBranchesWithPR", is(false))
                        )
                )
        );
    }

    @Test
    public void build_100001() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(true)),
                                hasProperty("buildBranchesWithPR", is(false))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(2))
                        )
                )
        );
    }

    @Test
    public void build_100010() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(true)),
                                hasProperty("buildBranchesWithPR", is(false))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(1))
                        )
                )
        );
    }

    @Test
    public void build_100011() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(true)),
                                hasProperty("buildBranchesWithPR", is(false))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(3))
                        )
                )
        );
    }

    @Test
    public void build_100100() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(true)),
                                hasProperty("buildBranchesWithPR", is(false))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(2))
                        )
                )
        );
    }

    @Test
    public void build_100101() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(true)),
                                hasProperty("buildBranchesWithPR", is(false))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(2))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(2))
                        )
                )
        );
    }

    @Test
    public void build_100110() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(true)),
                                hasProperty("buildBranchesWithPR", is(false))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(2))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(1))
                        )
                )
        );
    }

    @Test
    public void build_100111() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(true)),
                                hasProperty("buildBranchesWithPR", is(false))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(2))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(3))
                        )
                )
        );
    }

    @Test
    public void build_101000() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(true)),
                                hasProperty("buildBranchesWithPR", is(false))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(1))
                        )
                )
        );
    }

    @Test
    public void build_101001() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(true)),
                                hasProperty("buildBranchesWithPR", is(false))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(1))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(2))
                        )
                )
        );
    }

    @Test
    public void build_101010() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(true)),
                                hasProperty("buildBranchesWithPR", is(false))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(1))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(1))
                        )
                )
        );
    }

    @Test
    public void build_101011() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(true)),
                                hasProperty("buildBranchesWithPR", is(false))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(1))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(3))
                        )
                )
        );
    }

    @Test
    public void build_101100() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(true)),
                                hasProperty("buildBranchesWithPR", is(false))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(3))
                        )
                )
        );
    }

    @Test
    public void build_101101() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(true)),
                                hasProperty("buildBranchesWithPR", is(false))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(3))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(2))
                        )
                )
        );
    }

    @Test
    public void build_101110() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(true)),
                                hasProperty("buildBranchesWithPR", is(false))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(3))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(1))
                        )
                )
        );
    }

    @Test
    public void build_101111() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(true)),
                                hasProperty("buildBranchesWithPR", is(false))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(3))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(3))
                        )
                )
        );
    }

    @Test
    public void build_110000() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                contains(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(true)),
                                hasProperty("buildBranchesWithPR", is(true))
                        )
                )
        );
    }

    @Test
    public void build_110001() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(true)),
                                hasProperty("buildBranchesWithPR", is(true))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(2))
                        )
                )
        );
    }

    @Test
    public void build_110010() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(true)),
                                hasProperty("buildBranchesWithPR", is(true))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(1))
                        )
                )
        );
    }

    @Test
    public void build_110011() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(true)),
                                hasProperty("buildBranchesWithPR", is(true))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(3))
                        )
                )
        );
    }

    @Test
    public void build_110100() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(true)),
                                hasProperty("buildBranchesWithPR", is(true))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(2))
                        )
                )
        );
    }

    @Test
    public void build_110101() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(true)),
                                hasProperty("buildBranchesWithPR", is(true))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(2))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(2))
                        )
                )
        );
    }

    @Test
    public void build_110110() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(true)),
                                hasProperty("buildBranchesWithPR", is(true))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(2))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(1))
                        )
                )
        );
    }

    @Test
    public void build_110111() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(true)),
                                hasProperty("buildBranchesWithPR", is(true))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(2))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(3))
                        )
                )
        );
    }

    @Test
    public void build_111000() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(true)),
                                hasProperty("buildBranchesWithPR", is(true))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(1))
                        )
                )
        );
    }

    @Test
    public void build_111001() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(true)),
                                hasProperty("buildBranchesWithPR", is(true))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(1))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(2))
                        )
                )
        );
    }

    @Test
    public void build_111010() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(true)),
                                hasProperty("buildBranchesWithPR", is(true))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(1))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(1))
                        )
                )
        );
    }

    @Test
    public void build_111011() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(true)),
                                hasProperty("buildBranchesWithPR", is(true))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(1))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(3))
                        )
                )
        );
    }

    @Test
    public void build_111100() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(true)),
                                hasProperty("buildBranchesWithPR", is(true))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(3))
                        )
                )
        );
    }

    @Test
    public void build_111101() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(true)),
                                hasProperty("buildBranchesWithPR", is(true))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(3))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(2))
                        )
                )
        );
    }

    @Test
    public void build_111110() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(true)),
                                hasProperty("buildBranchesWithPR", is(true))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(3))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(1))
                        )
                )
        );
    }

    @Test
    public void build_111111() throws Exception {
        AzureDevOpsRepoSCMSource instance = load();
        assertThat(instance.getTraits(),
                containsInAnyOrder(
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(BranchDiscoveryTrait.class),
                                hasProperty("buildBranch", is(true)),
                                hasProperty("buildBranchesWithPR", is(true))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(OriginPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(3))
                        ),
                        Matchers.<SCMSourceTrait>allOf(
                                instanceOf(ForkPullRequestDiscoveryTrait.class),
                                hasProperty("strategyId", is(3))
                        )
                )
        );
    }
}
