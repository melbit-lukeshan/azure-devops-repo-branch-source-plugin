package org.jenkinsci.plugins.azure_devops_repo_branch_source;

import edu.umd.cs.findbugs.annotations.NonNull;
import jenkins.plugins.git.GitTagSCMHead;
import jenkins.scm.api.mixin.TagSCMHead;

public class AzureDevOpsRepoTagSCMHead extends GitTagSCMHead implements TagSCMHead {

    /**
     * Constructor.
     *
     * @param name      the name.
     * @param timestamp the tag timestamp;
     */
    public AzureDevOpsRepoTagSCMHead(@NonNull String name, long timestamp) {
        super(name, timestamp);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPronoun() {
        return Messages.AzureDevOpsRepoTagSCMHead_Pronoun();
    }

}
