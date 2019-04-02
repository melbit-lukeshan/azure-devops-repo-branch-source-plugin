//CHECKSTYLE:OFF
package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model;

import hudson.model.Action;
import hudson.model.Run;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.io.Serializable;
import java.net.URI;
import java.util.Collections;
import java.util.List;

/**
 * Captures details of the TFS/Team Services pull request event which triggered us.
 */
@ExportedBean(defaultVisibility = 999)
public class TeamPullRequestMergedDetailsAction implements Action, Serializable {
    private static final long serialVersionUID = 1L;
    private static final String URL_NAME = "team-pullRequestMergedDetails";

    public transient GitPullRequest gitPullRequest;
    public String message;
    public String detailedMessage;
    public String collectionUri;

    public TeamPullRequestMergedDetailsAction() {

    }

    public TeamPullRequestMergedDetailsAction(final GitPullRequest gitPullRequest, final String message, final String detailedMessage, final String collectionUri) {
        this.gitPullRequest = gitPullRequest;
        this.message = message;
        this.detailedMessage = detailedMessage;
        this.collectionUri = collectionUri;
    }

    public static URI addWorkItemsForRun(final Run<?, ?> run, final List<ResourceRef> destination) {
        final TeamPullRequestMergedDetailsAction action = run.getAction(TeamPullRequestMergedDetailsAction.class);
        if (action != null && action.hasWorkItems()) {
            Collections.addAll(destination, action.getWorkItems());
            final GitPullRequest gitPullRequest = action.gitPullRequest;
            final URI collectionUri = URI.create(action.collectionUri);
            return collectionUri;
        }
        return null;
    }

    @Override
    public String getIconFileName() {
        return "/plugin/tfs/48x48/logo.png";
    }

    @Override
    public String getDisplayName() {
        return "TFS/Team Services pull request";
    }

    @Override
    public String getUrlName() {
        return URL_NAME;
    }

    // the following methods are called from this/summary.jelly and/or this/index.jelly

    @Exported
    public String getMessage() {
        return message;
    }

    @Exported
    public String getDetailedMessage() {
        return detailedMessage;
    }

    @Exported
    public ResourceRef[] getWorkItems() {
        return gitPullRequest.getWorkItemRefs().toArray(new ResourceRef[0]);
    }

    @Exported
    public boolean hasWorkItems() {
        final ResourceRef[] workItemRefs = gitPullRequest.getWorkItemRefs().toArray(new ResourceRef[0]);
        return workItemRefs != null && workItemRefs.length > 0;
    }

    @Exported
    public String getPullRequestUrl() {
        final GitRepository repository = gitPullRequest.getRepository();
        final URI collectionUri = URI.create(this.collectionUri);
        final TeamProjectReference project = repository.getProject();
        final URI pullRequestUrl = UriHelper.join(collectionUri,
                project.getName(),
                "_git",
                repository.getName(),
                "pullrequest",
                gitPullRequest.getPullRequestId()
        );
        final String result = pullRequestUrl.toString();
        return result;
    }
}
