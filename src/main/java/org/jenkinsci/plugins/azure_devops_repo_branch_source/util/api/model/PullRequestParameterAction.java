package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model;

/**
 * Action that adds the pull request merge event args to the build information.
 */
public class PullRequestParameterAction extends CommitParameterAction {

    private final PullRequestMergeCommitCreatedEventArgs args;

    public PullRequestParameterAction(final PullRequestMergeCommitCreatedEventArgs args) {
        super(args);
        this.args = args;
    }

    public PullRequestMergeCommitCreatedEventArgs getPullRequestMergeCommitCreatedEventArgs() {
        return args;
    }
}
