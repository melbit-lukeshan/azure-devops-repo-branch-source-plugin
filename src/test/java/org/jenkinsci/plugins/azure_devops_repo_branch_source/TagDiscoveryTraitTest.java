package org.jenkinsci.plugins.azure_devops_repo_branch_source;

import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMHeadObserver;
import jenkins.scm.api.SCMHeadOrigin;
import jenkins.scm.api.mixin.ChangeRequestCheckoutStrategy;
import jenkins.scm.api.trait.SCMHeadAuthority;
import jenkins.scm.impl.ChangeRequestSCMHeadCategory;
import jenkins.scm.impl.TagSCMHeadCategory;
import jenkins.scm.impl.UncategorizedSCMHeadCategory;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class TagDiscoveryTraitTest {
    @ClassRule
    public static JenkinsRule j = new JenkinsRule();

    @Test
    public void decorateContext() throws Exception {
        AzureDevOpsRepoSCMSourceContext probe = new AzureDevOpsRepoSCMSourceContext(null, SCMHeadObserver.collect());
        assertThat(probe.wantBranches(), is(false));
        assertThat(probe.wantPRs(), is(false));
        assertThat(probe.wantTags(), is(false));
        assertThat(probe.authorities(), is(Collections.<SCMHeadAuthority<?, ?, ?>>emptyList()));
        new TagDiscoveryTrait().applyToContext(probe);
        assertThat(probe.wantBranches(), is(false));
        assertThat(probe.wantPRs(), is(false));
        assertThat(probe.wantTags(), is(true));
        assertThat(probe.authorities(), contains(instanceOf(TagDiscoveryTrait.TagSCMHeadAuthority.class)));
    }

    @Test
    public void includeCategory() throws Exception {
        assertThat(new TagDiscoveryTrait().includeCategory(ChangeRequestSCMHeadCategory.DEFAULT), is(false));
        assertThat(new TagDiscoveryTrait().includeCategory(UncategorizedSCMHeadCategory.DEFAULT), is(false));
        assertThat(new TagDiscoveryTrait().includeCategory(TagSCMHeadCategory.DEFAULT), is(true));
    }

    @Test
    public void authority() throws Exception {
        try (AzureDevOpsRepoSCMSourceRequest probe = new AzureDevOpsRepoSCMSourceContext(null, SCMHeadObserver.collect()).newRequest(new AzureDevOpsRepoSCMSource("does-not-exist", "http://does-not-exist.test"), null)) {
            TagDiscoveryTrait.TagSCMHeadAuthority instance = new TagDiscoveryTrait.TagSCMHeadAuthority();
            assertThat(instance.isTrusted(probe, new SCMHead("v1.0.0")), is(false));
            assertThat(instance.isTrusted(probe, new PullRequestSCMHead("PR-1", "does-not-exists",
                    "http://does-not-exist.test", "feature/1", 1, new BranchSCMHead("master"), SCMHeadOrigin.DEFAULT, ChangeRequestCheckoutStrategy.MERGE)), is(false));
            assertThat(instance.isTrusted(probe, new AzureDevOpsRepoTagSCMHead("v1.0.0", 0L)), is(true));
        }
    }

}
