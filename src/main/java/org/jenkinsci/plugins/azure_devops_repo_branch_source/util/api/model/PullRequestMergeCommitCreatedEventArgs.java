//CHECKSTYLE:OFF
package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model;

public class PullRequestMergeCommitCreatedEventArgs extends GitCodePushedEventArgs {

    public int pullRequestId;
    public int iterationId = -1;

}
