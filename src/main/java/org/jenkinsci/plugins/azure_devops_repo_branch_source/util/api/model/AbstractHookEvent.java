package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.model.*;
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.GitStatus;
import hudson.plugins.git.UserRemoteConfig;
import hudson.scm.SCM;
import hudson.security.ACL;
import hudson.triggers.SCMTrigger;
import jenkins.model.Jenkins;
import jenkins.scm.api.SCMSource;
import jenkins.triggers.SCMTriggerItem;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.transport.URIish;
import org.jenkinsci.plugins.azure_devops_repo_branch_source.AzureDevOpsRepoSCMSource;
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.ActionHelper;
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition;
import org.jenkinsci.plugins.workflow.flow.FlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * This class abstracts the hook event.
 */
public abstract class AbstractHookEvent {

    private static final Logger LOGGER = Logger.getLogger(AbstractHookEvent.class.getName());
    private static final String TRIGGER_ANY_BRANCH = "**";

    static JSONObject fromResponseContributors(final List<GitStatus.ResponseContributor> contributors) {
        final JSONObject result = new JSONObject();
        final JSONArray messages = new JSONArray();
        for (final GitStatus.ResponseContributor contributor : contributors) {
            final StringWriter stringWriter = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(stringWriter);
            try {
                contributor.writeBody(printWriter);
                printWriter.flush();
            } finally {
                IOUtils.closeQuietly(printWriter);
            }
            final String contributorMessage = stringWriter.toString();
            messages.add(contributorMessage);
        }
        result.put("messages", messages);
        return result;
    }

    /**
     * Actually do the work of the hook event, using the supplied
     * {@code mapper} to convert the event's data from the supplied {@code serviceHookEvent}
     * and returning the output as a {@link JSONObject}.
     *
     * @param mapper           an {@link ObjectMapper} instance to use to convert the {@link Event#getResource()}
     * @param serviceHookEvent an {@link Event} that represents the request payload
     *                         and from which the {@link Event#getResource()} can be obtained
     * @param message          a simple description of the event
     * @param detailedMessage  a longer description of the event, with some details
     * @return a {@link JSONObject} representing the hook event's output
     */
    public abstract JSONObject perform(final ObjectMapper mapper, final Event serviceHookEvent, final String message, final String detailedMessage);

    GitStatus.ResponseContributor triggerJob(final GitCodePushedEventArgs gitCodePushedEventArgs, final List<Action> actions, final boolean bypassPolling, final Item project, final SCMTriggerItem scmTriggerItem, final Boolean repoMatches, final Boolean branchMatches) {
        if (!(project instanceof AbstractProject && ((AbstractProject) project).isDisabled())) {
            if (project instanceof Job) {
                final Job job = (Job) project;

                final ArrayList<ParameterValue> values = getDefaultParameters(job);
                final String vstsRefspec = getVstsRefspec(gitCodePushedEventArgs);
                values.add(new StringParameterValue("vstsRefspec", vstsRefspec));
                values.add(new StringParameterValue("vstsBranchOrCommit", gitCodePushedEventArgs.commit));
                SafeParametersAction paraAction = new SafeParametersAction(values);
                final Action[] actionsNew = ActionHelper.create(actions, paraAction);
                final List<Action> actionsWithSafeParams = new ArrayList<>(Arrays.asList(actionsNew));

                final SCMTrigger scmTrigger = AzureDevOpsEventsEndpoint.findTrigger(job, SCMTrigger.class);
                if (scmTrigger == null || !scmTrigger.isIgnorePostCommitHooks()) {
                    // trigger is null OR job does NOT have explicitly opted out of hooks
                    TeamPushTrigger trigger;
                    if (gitCodePushedEventArgs instanceof PullRequestMergeCommitCreatedEventArgs) {
                        trigger = new TeamPRPushTrigger(job, gitCodePushedEventArgs.targetBranch, null);
                    } else {
                        trigger = new TeamPushTrigger(job);
                    }
                    trigger.execute(gitCodePushedEventArgs, actionsWithSafeParams, bypassPolling);
                    if (bypassPolling) {
                        return new AzureDevOpsEventsEndpoint.ScheduledResponseContributor(project);
                    } else {
                        return new AzureDevOpsEventsEndpoint.PollingScheduledResponseContributor(project);
                    }
                }
            }
        }

        return null;
    }

    private void matchMultiBranchProject(final GitCodePushedEventArgs gitCodePushedEventArgs, final List<Action> actions, final boolean bypassPolling, final URIish uri, final WorkflowMultiBranchProject wmbp, final List<GitStatus.ResponseContributor> result, final MatchStatus matchStatus) {
        boolean repositoryMatches = false;
        //TODO debug - Luke
        if (gitCodePushedEventArgs instanceof PullRequestMergeCommitCreatedEventArgs) {
            PullRequestMergeCommitCreatedEventArgs p = (PullRequestMergeCommitCreatedEventArgs) gitCodePushedEventArgs;
            System.out.println("matchMultiBranchProject PullRequestMergeCommitCreatedEventArgs pullRequestId " + p.pullRequestId);
            System.out.println("matchMultiBranchProject PullRequestMergeCommitCreatedEventArgs targetBranch " + p.targetBranch);
            System.out.println("matchMultiBranchProject PullRequestMergeCommitCreatedEventArgs commit " + p.commit);
        } else {
            System.out.println("matchMultiBranchProject GitCodePushedEventArgs targetBranch " + gitCodePushedEventArgs.targetBranch);
            System.out.println("matchMultiBranchProject GitCodePushedEventArgs commit " + gitCodePushedEventArgs.commit);
        }
        //TODO debug end - Luke
        for (SCMSource scmSource : wmbp.getSCMSources()) {
            if (scmSource instanceof AzureDevOpsRepoSCMSource) {
                AzureDevOpsRepoSCMSource gitSCMSource = (AzureDevOpsRepoSCMSource) scmSource;
                System.out.println("matchMultiBranchProject gitSCMSource " + gitSCMSource.getRemote());
                try {
                    if (UriHelper.areSameGitRepo(uri, new URIish(gitSCMSource.getRemote()))) {
                        repositoryMatches = true;
                        matchStatus.repoMatchFound++;
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (repositoryMatches) {
            for (WorkflowJob workflowJob : wmbp.getItems()) {
                final String branchName = workflowJob.getName();

                final SCMTriggerItem scmTriggerItem = SCMTriggerItem.SCMTriggerItems.asSCMTriggerItem(workflowJob);

                if (scmTriggerItem != null) {
                    if (gitCodePushedEventArgs instanceof PullRequestMergeCommitCreatedEventArgs) {
                        PullRequestMergeCommitCreatedEventArgs p = (PullRequestMergeCommitCreatedEventArgs) gitCodePushedEventArgs;
                        if (branchName.equalsIgnoreCase("PR-" + p.pullRequestId)) {
                            System.out.println("matchMultiBranchProject match PR:" + branchName);
                            matchStatus.branchMatchFound++;
                            GitStatus.ResponseContributor triggerResult = triggerJob(p, actions, bypassPolling, workflowJob, scmTriggerItem, true, false);
                            if (triggerResult != null) {
                                result.add(triggerResult);
                            }
                            break;
                        }
                    } else {
                        if (branchName.equalsIgnoreCase(gitCodePushedEventArgs.targetBranch)) {
                            System.out.println("matchMultiBranchProject match PUSH:" + branchName);
                            matchStatus.branchMatchFound++;
                            GitStatus.ResponseContributor triggerResult = triggerJob(gitCodePushedEventArgs, actions, bypassPolling, workflowJob, scmTriggerItem, true, true);
                            if (triggerResult != null) {
                                result.add(triggerResult);
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    private void matchProject(final GitCodePushedEventArgs gitCodePushedEventArgs, final List<Action> actions, final boolean bypassPolling, final URIish uri, final Item project, final List<GitStatus.ResponseContributor> result, final MatchStatus matchStatus) {
        //TODO debug - Luke
        System.out.println("matchProject project " + project);
        System.out.println("matchProject project.getFullDisplayName() " + project.getFullDisplayName());
        final SCMTriggerItem scmTriggerItem = SCMTriggerItem.SCMTriggerItems.asSCMTriggerItem(project);

        if (scmTriggerItem == null) {
            if (project instanceof WorkflowMultiBranchProject) {
                WorkflowMultiBranchProject wmbp = (WorkflowMultiBranchProject) project;
                matchMultiBranchProject(gitCodePushedEventArgs, actions, bypassPolling, uri, wmbp, result, matchStatus);
            } else {
//                if (project instanceof Folder) {
//                    for (Job job : project.getAllJobs()) {
//                        matchProject(gitCodePushedEventArgs, actions, bypassPolling, uri, job, result, matchStatus);
//                    }
//                }
            }
        } else {
            // Pipeline job
//            if (scmTriggerItem.getSCMs().isEmpty()) {
//                return;
//            }
//
//            for (final SCM scm : scmTriggerItem.getSCMs()) {
//                if (!(scm instanceof GitSCM)) {
//                    continue;
//                }
//                final GitSCM git = (GitSCM) scm;
//                matchStatus.scmFound = true;
//
//                for (final RemoteConfig repository : git.getRepositories()) {
//                    boolean repositoryMatches = false;
//                    for (final URIish remoteURL : repository.getURIs()) {
//                        if (UriHelper.areSameGitRepo(uri, remoteURL)) {
//                            repositoryMatches = true;
//                            matchStatus.repoMatchFound++;
//                            break;
//                        }
//                    }
//
//                    // Jobs triggered by PR merge need to check whether its target branch matches the one specified in the parameter of PR trigger UI
//                    if (repositoryMatches && gitCodePushedEventArgs instanceof PullRequestMergeCommitCreatedEventArgs) {
//                        GitStatus.ResponseContributor triggerResult = triggerJob(gitCodePushedEventArgs, actions, bypassPolling, project, scmTriggerItem, true, false);
//                        if (triggerResult != null) {
//                            result.add(triggerResult);
//                        }
//                        break;
//                    }
//
//                    boolean branchMatches = false;
//                    // Jobs triggered by code push need to check whether its target branch matches the one in its Git parameter
//                    if (repositoryMatches) {
//                        for (final BranchSpec branch : git.getBranches()) {
//                            final String branchString = branch.toString();
//                            // Might be in the form of */master
//                            final String[] items = branchString.split("/");
//                            final String branchName = items[items.length - 1];
//                            if (branchName.equalsIgnoreCase(TRIGGER_ANY_BRANCH) || branchName.equalsIgnoreCase(gitCodePushedEventArgs.targetBranch)) {
//                                branchMatches = true;
//                                matchStatus.branchMatchFound++;
//                                break;
//                            }
//                        }
//                    }
//
//                    if (!repositoryMatches || !branchMatches || git.getExtensions().get(IgnoreNotifyCommit.class) != null) {
//                        continue;
//                    }
//
//                    GitStatus.ResponseContributor triggerResult = triggerJob(gitCodePushedEventArgs, actions, bypassPolling, project, scmTriggerItem, true, true);
//                    if (triggerResult != null) {
//                        result.add(triggerResult);
//                        break;
//                    }
//                }
//            }
        }
    }

    private Boolean repoMatches(final GitCodePushedEventArgs gitCodePushedEventArgs, final Job job) {
        if (job instanceof WorkflowJob) {
            final FlowDefinition jobDef = ((WorkflowJob) job).getDefinition();
            if (jobDef instanceof CpsScmFlowDefinition) {
                final SCM jobSCM = ((CpsScmFlowDefinition) jobDef).getScm();
                if (jobSCM instanceof GitSCM) {
                    final GitSCM gitJobSCM = (GitSCM) jobSCM;
                    final URIish uri = gitCodePushedEventArgs.getRepoURIish();

                    for (final UserRemoteConfig remoteConfig : gitJobSCM.getUserRemoteConfigs()) {
                        final String jobRepoUrl = remoteConfig.getUrl();

                        if (StringUtils.equalsIgnoreCase(jobRepoUrl, uri.toString())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private Boolean branchMatches(final GitCodePushedEventArgs gitCodePushedEventArgs, final Job job) {
        // Jobs triggered by PR merge need to check whether its target branch matches the one specified in the parameter of PR trigger UI
        if (gitCodePushedEventArgs instanceof PullRequestMergeCommitCreatedEventArgs) {
            TeamPRPushTrigger pushTrigger = AzureDevOpsEventsEndpoint.findTrigger(job, TeamPRPushTrigger.class);
            if (pushTrigger != null) {
                final String targetBranches = pushTrigger.getTargetBranches();
                if (targetBranches != null) {
                    String[] branches = targetBranches.split(" ");
                    if (branches != null) {
                        for (String branchFullName : branches) {
                            // branchFullName could be in the format of */pr_status
                            String[] items = branchFullName.split("/");
                            if (StringUtils.equalsIgnoreCase(items[items.length - 1], gitCodePushedEventArgs.targetBranch)) {
                                return true;
                            }
                        }
                    }
                }
            }
        } else { // Pipeline jobs triggered by code push need to check whether its target branch matches the one in its Git parameter
            if (job instanceof WorkflowJob) {
                final FlowDefinition jobDef = ((WorkflowJob) job).getDefinition();
                if (jobDef instanceof CpsScmFlowDefinition) {
                    final SCM jobSCM = ((CpsScmFlowDefinition) jobDef).getScm();
                    if (jobSCM instanceof GitSCM) {
                        final GitSCM gitJobSCM = (GitSCM) jobSCM;
                        for (final BranchSpec branchFullName : gitJobSCM.getBranches()) {
                            // branchFullName could be in the format of */pr_status
                            String[] items = branchFullName.getName().split("/");
                            if (StringUtils.equalsIgnoreCase(items[items.length - 1], gitCodePushedEventArgs.targetBranch)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    List<GitStatus.ResponseContributor> pollOrQueueFromEvent(final GitCodePushedEventArgs gitCodePushedEventArgs, final List<Action> actions, final boolean bypassPolling) {
        List<GitStatus.ResponseContributor> result = new ArrayList<>();
        final String commit = gitCodePushedEventArgs.commit;
        if (commit == null) {
            result.add(new GitStatus.MessageResponseContributor("No commits were pushed, skipping further event processing."));
            return result;
        }
        final URIish uri = gitCodePushedEventArgs.getRepoURIish();

        SecurityContext old = ACL.impersonate(ACL.SYSTEM);
        try {
            MatchStatus matchStatus = new MatchStatus();
            for (final Item project : Jenkins.get().getAllItems(WorkflowMultiBranchProject.class)) {
                matchProject(gitCodePushedEventArgs, actions, bypassPolling, uri, project, result, matchStatus);
            }
            if (!matchStatus.scmFound) {
                result.add(new GitStatus.MessageResponseContributor("No Git jobs found"));
            } else if (matchStatus.repoMatchFound == 0 && matchStatus.branchMatchFound == 0) {
                final String template = "No Git jobs matched the remote URL '%s' requested by an event.";
                final String message = String.format(template, uri);
                LOGGER.warning(message);
            }

            return result;
        } finally {
            SecurityContextHolder.setContext(old);
        }
    }

    private ArrayList<ParameterValue> getDefaultParameters(final Job<?, ?> job) {
        ArrayList<ParameterValue> values = new ArrayList<>();
        ParametersDefinitionProperty pdp = job.getProperty(ParametersDefinitionProperty.class);
        if (pdp != null) {
            for (ParameterDefinition pd : pdp.getParameterDefinitions()) {
                if (pd.getName().equals("sha1")) {
                    continue;
                }
                values.add(pd.getDefaultParameterValue());
            }
        }
        return values;
    }

    private String getVstsRefspec(final GitCodePushedEventArgs gitCodePushedEventArgs) {
        if (gitCodePushedEventArgs instanceof PullRequestMergeCommitCreatedEventArgs) {
            int prId = ((PullRequestMergeCommitCreatedEventArgs) gitCodePushedEventArgs).pullRequestId;
            return String.format("+refs/pull/%d/merge:refs/remotes/origin-pull/%d/merge", prId, prId);
        } else {
            return "+refs/heads/*:refs/remotes/origin/*";
        }
    }

    /**
     * Interface of hook event factory.
     */
    public interface Factory {
        /**
         * Create the factory.
         */
        AbstractHookEvent create();

        /**
         * Get sample request payload.
         */
        String getSampleRequestPayload();
    }

    class MatchStatus {
        public boolean scmFound = false;
        public int repoMatchFound = 0;
        public int branchMatchFound = 0;
    }
}
