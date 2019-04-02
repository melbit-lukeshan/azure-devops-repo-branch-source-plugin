package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model;

import hudson.plugins.git.RevisionParameterAction;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;

import java.net.URI;

/**
 * Used as a build parameter to record information about the associated project and
 * Visual Studio Team Services account or TFS server to facilitate integration.
 */
public class CommitParameterAction extends RevisionParameterAction {

    private final GitCodePushedEventArgs gitCodePushedEventArgs;

    /**
     * Saves the repo uri and GitCodePushedEventArgs.
     */
    public CommitParameterAction(final GitCodePushedEventArgs e) {
        super(e.commit, e.getRepoURIish());

        this.gitCodePushedEventArgs = e;
    }

    /**
     * Returns the GitCodePushedEventArgs.
     */
    public GitCodePushedEventArgs getGitCodePushedEventArgs() {
        return gitCodePushedEventArgs;
    }

    /**
     * Returns true if the git repo uri can originate from any of the remotes passed in.
     */
    @Override
    public boolean canOriginateFrom(final Iterable<RemoteConfig> remotes) {
        final URI repoUri = gitCodePushedEventArgs.repoUri;
        for (final RemoteConfig remote : remotes) {
            for (final URIish remoteURL : remote.getURIs()) {
                try {
                    final URI remoteUri = URI.create(remoteURL.toString());
                    if (UriHelper.areSameGitRepo(remoteUri, repoUri)) {
                        return true;
                    }
                } catch (Exception e) {
                }
            }
        }
        return false;
    }
}
