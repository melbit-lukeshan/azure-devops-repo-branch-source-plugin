package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model

import hudson.Extension
import hudson.model.Descriptor
import hudson.plugins.git.GitChangeSet
import hudson.plugins.git.browser.GitRepositoryBrowser
import hudson.scm.EditType
import hudson.scm.RepositoryBrowser
import net.sf.json.JSONObject
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.StaplerRequest
import java.io.IOException
import java.net.URL

class AzureDevOpsWeb @DataBoundConstructor
constructor(repoUrl: String) : GitRepositoryBrowser(repoUrl) {

    @Throws(IOException::class)
    override fun getChangeSetLink(changeSet: GitChangeSet): URL {
        val url = this.url
        return URL(url, url.path + "commit/" + changeSet.id)
    }

    @Throws(IOException::class)
    override
    fun getDiffLink(path: GitChangeSet.Path): URL? {
        return if (path.editType == EditType.EDIT && path.src != null && path.dst != null && path.changeSet.parentCommit != null) this.getDiffLinkRegardlessOfEditType(path) else null
    }

    @Throws(IOException::class)
    private fun getDiffLinkRegardlessOfEditType(path: GitChangeSet.Path): URL {
        return URL(this.getChangeSetLink(path.changeSet), "#diff-" + this.getIndexOfPath(path).toString())
    }

    @Throws(IOException::class)
    override
    fun getFileLink(path: GitChangeSet.Path): URL {
        if (path.editType == EditType.DELETE) {
            return this.getDiffLinkRegardlessOfEditType(path)
        } else {
            val spec = "blob/" + path.changeSet.id + "/" + path.path
            val url = this.url
            return URL(url, url.path + spec)
        }
    }

    @Extension
    class AzureDevOpsWebDescriptor : Descriptor<RepositoryBrowser<*>>() {

        override fun getDisplayName(): String {
            return "azureDevOpsWeb"
        }

        @Throws(hudson.model.Descriptor.FormException::class)
        override fun newInstance(req: StaplerRequest?, jsonObject: JSONObject): AzureDevOpsWeb {
            assert(req != null)

            return req!!.bindJSON(AzureDevOpsWeb::class.java, jsonObject) as AzureDevOpsWeb
        }
    }

    companion object {
        private val serialVersionUID = 1L
    }
}
