package org.jenkinsci.plugins.azure_devops_repo_branch_source;

import hudson.util.XStream2;
import jenkins.scm.api.SCMHeadObserver;
import jenkins.scm.api.mixin.ChangeRequestCheckoutStrategy;
import jenkins.scm.api.trait.SCMHeadFilter;
import jenkins.scm.api.trait.SCMHeadPrefilter;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

public class ForkPullRequestDiscoveryTraitTest {
    @Test
    public void xstream() throws Exception  {
        System.out.println(new XStream2().toXML(new ForkPullRequestDiscoveryTrait(3, new ForkPullRequestDiscoveryTrait.TrustContributors())));
    }

    @Test
    public void given__disoverHeadMerge__when__appliedToContext__then__strategiesCorrect() throws Exception {
        AzureDevOpsRepoSCMSourceContext ctx = new AzureDevOpsRepoSCMSourceContext(null, SCMHeadObserver.none());
        assumeThat(ctx.wantBranches(), is(false));
        assumeThat(ctx.wantPRs(), is(false));
        assumeThat(ctx.prefilters(), is(Collections.<SCMHeadPrefilter>emptyList()));
        assumeThat(ctx.filters(), is(Collections.<SCMHeadFilter>emptyList()));
        assumeThat(ctx.authorities(), not((Matcher) hasItem(
                instanceOf(ForkPullRequestDiscoveryTrait.TrustContributors.class)
        )));
        ForkPullRequestDiscoveryTrait instance = new ForkPullRequestDiscoveryTrait(
                EnumSet.allOf(ChangeRequestCheckoutStrategy.class),
                new ForkPullRequestDiscoveryTrait.TrustContributors()
        );
        instance.decorateContext(ctx);
        assertThat(ctx.wantBranches(), is(false));
        assertThat(ctx.wantPRs(), is(true));
        assertThat(ctx.prefilters(), is(Collections.<SCMHeadPrefilter>emptyList()));
        assertThat(ctx.filters(), is(Collections.<SCMHeadFilter>emptyList()));
        assertThat(ctx.forkPRStrategies(),
                Matchers.<Set<ChangeRequestCheckoutStrategy>>is(EnumSet.allOf(ChangeRequestCheckoutStrategy.class)));
        assertThat(ctx.authorities(), (Matcher) hasItem(
                instanceOf(ForkPullRequestDiscoveryTrait.TrustContributors.class)
        ));
    }

    @Test
    public void given__disoverHeadOnly__when__appliedToContext__then__strategiesCorrect() throws Exception {
        AzureDevOpsRepoSCMSourceContext ctx = new AzureDevOpsRepoSCMSourceContext(null, SCMHeadObserver.none());
        assumeThat(ctx.wantBranches(), is(false));
        assumeThat(ctx.wantPRs(), is(false));
        assumeThat(ctx.prefilters(), is(Collections.<SCMHeadPrefilter>emptyList()));
        assumeThat(ctx.filters(), is(Collections.<SCMHeadFilter>emptyList()));
        assumeThat(ctx.authorities(), not((Matcher) hasItem(
                instanceOf(ForkPullRequestDiscoveryTrait.TrustContributors.class)
        )));
        ForkPullRequestDiscoveryTrait instance = new ForkPullRequestDiscoveryTrait(
                EnumSet.of(ChangeRequestCheckoutStrategy.HEAD),
                new ForkPullRequestDiscoveryTrait.TrustContributors()
        );
        instance.decorateContext(ctx);
        assertThat(ctx.wantBranches(), is(false));
        assertThat(ctx.wantPRs(), is(true));
        assertThat(ctx.prefilters(), is(Collections.<SCMHeadPrefilter>emptyList()));
        assertThat(ctx.filters(), is(Collections.<SCMHeadFilter>emptyList()));
        assertThat(ctx.forkPRStrategies(),
                Matchers.<Set<ChangeRequestCheckoutStrategy>>is(EnumSet.of(ChangeRequestCheckoutStrategy.HEAD)));
        assertThat(ctx.authorities(), (Matcher) hasItem(
                instanceOf(ForkPullRequestDiscoveryTrait.TrustContributors.class)
        ));
    }

    @Test
    public void given__disoverMergeOnly__when__appliedToContext__then__strategiesCorrect() throws Exception {
        AzureDevOpsRepoSCMSourceContext ctx = new AzureDevOpsRepoSCMSourceContext(null, SCMHeadObserver.none());
        assumeThat(ctx.wantBranches(), is(false));
        assumeThat(ctx.wantPRs(), is(false));
        assumeThat(ctx.prefilters(), is(Collections.<SCMHeadPrefilter>emptyList()));
        assumeThat(ctx.filters(), is(Collections.<SCMHeadFilter>emptyList()));
        assumeThat(ctx.authorities(), not((Matcher) hasItem(
                instanceOf(ForkPullRequestDiscoveryTrait.TrustContributors.class)
        )));
        ForkPullRequestDiscoveryTrait instance = new ForkPullRequestDiscoveryTrait(
                EnumSet.of(ChangeRequestCheckoutStrategy.MERGE),
                new ForkPullRequestDiscoveryTrait.TrustContributors()
        );
        instance.decorateContext(ctx);
        assertThat(ctx.wantBranches(), is(false));
        assertThat(ctx.wantPRs(), is(true));
        assertThat(ctx.prefilters(), is(Collections.<SCMHeadPrefilter>emptyList()));
        assertThat(ctx.filters(), is(Collections.<SCMHeadFilter>emptyList()));
        assertThat(ctx.forkPRStrategies(),
                Matchers.<Set<ChangeRequestCheckoutStrategy>>is(EnumSet.of(ChangeRequestCheckoutStrategy.MERGE)));
        assertThat(ctx.authorities(), (Matcher) hasItem(
                instanceOf(ForkPullRequestDiscoveryTrait.TrustContributors.class)
        ));
    }

    @Test
    public void given__nonDefaultTrust__when__appliedToContext__then__authoritiesCorrect() throws Exception {
        AzureDevOpsRepoSCMSourceContext ctx = new AzureDevOpsRepoSCMSourceContext(null, SCMHeadObserver.none());
        assumeThat(ctx.wantBranches(), is(false));
        assumeThat(ctx.wantPRs(), is(false));
        assumeThat(ctx.prefilters(), is(Collections.<SCMHeadPrefilter>emptyList()));
        assumeThat(ctx.filters(), is(Collections.<SCMHeadFilter>emptyList()));
        assumeThat(ctx.authorities(), not((Matcher) hasItem(
                instanceOf(ForkPullRequestDiscoveryTrait.TrustContributors.class)
        )));
        ForkPullRequestDiscoveryTrait instance = new ForkPullRequestDiscoveryTrait(
                EnumSet.allOf(ChangeRequestCheckoutStrategy.class),
                new ForkPullRequestDiscoveryTrait.TrustEveryone()
        );
        instance.decorateContext(ctx);
        assertThat(ctx.wantBranches(), is(false));
        assertThat(ctx.wantPRs(), is(true));
        assertThat(ctx.prefilters(), is(Collections.<SCMHeadPrefilter>emptyList()));
        assertThat(ctx.filters(), is(Collections.<SCMHeadFilter>emptyList()));
        assertThat(ctx.forkPRStrategies(),
                Matchers.<Set<ChangeRequestCheckoutStrategy>>is(EnumSet.allOf(ChangeRequestCheckoutStrategy.class)));
        assertThat(ctx.authorities(), (Matcher) hasItem(
                instanceOf(ForkPullRequestDiscoveryTrait.TrustEveryone.class)
        ));
    }
}
