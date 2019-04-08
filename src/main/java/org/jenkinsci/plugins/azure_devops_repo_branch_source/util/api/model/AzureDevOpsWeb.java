package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.plugins.git.GitChangeSet;
import hudson.plugins.git.browser.GitRepositoryBrowser;
import hudson.scm.EditType;
import hudson.scm.RepositoryBrowser;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URL;


public class AzureDevOpsWeb extends GitRepositoryBrowser {
    private static final long serialVersionUID = 1L;

    @DataBoundConstructor
    public AzureDevOpsWeb(String repoUrl) {
        super(repoUrl);
    }

    public URL getChangeSetLink(GitChangeSet changeSet) throws IOException {
        URL url = this.getUrl();
        return new URL(url, url.getPath() + "commit/" + changeSet.getId());
    }

    public URL getDiffLink(GitChangeSet.Path path) throws IOException {
        return path.getEditType() == EditType.EDIT && path.getSrc() != null && path.getDst() != null && path.getChangeSet().getParentCommit() != null ? this.getDiffLinkRegardlessOfEditType(path) : null;
    }

    private URL getDiffLinkRegardlessOfEditType(GitChangeSet.Path path) throws IOException {
        return new URL(this.getChangeSetLink(path.getChangeSet()), "#diff-" + String.valueOf(this.getIndexOfPath(path)));
    }

    public URL getFileLink(GitChangeSet.Path path) throws IOException {
        if (path.getEditType().equals(EditType.DELETE)) {
            return this.getDiffLinkRegardlessOfEditType(path);
        } else {
            String spec = "blob/" + path.getChangeSet().getId() + "/" + path.getPath();
            URL url = this.getUrl();
            return new URL(url, url.getPath() + spec);
        }
    }

    @Extension
    public static class AzureDevOpsWebDescriptor extends Descriptor<RepositoryBrowser<?>> {
        public AzureDevOpsWebDescriptor() {
        }

        @Nonnull
        public String getDisplayName() {
            return "azuredevopsweb";
        }

        public AzureDevOpsWeb newInstance(StaplerRequest req, @Nonnull JSONObject jsonObject) throws FormException {
            assert req != null;

            return (AzureDevOpsWeb) req.bindJSON(AzureDevOpsWeb.class, jsonObject);
        }
    }
}
