package org.jenkinsci.plugins.azure_devops_repo_branch_source;

import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.*;
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model.*;
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.gson.GsonProcessor;
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.support.OkHttp2Helper;
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.support.Result;
import org.junit.Test;

import java.io.InputStream;
import java.time.OffsetDateTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;

public class AaaTest {
//    @ClassRule
//    public static JenkinsRule j = new JenkinsRule();

    public static final String collectionUrl = "https://dev.azure.com/lukeshan";
    public static final String pat = "ltwwf6dxrqhvjalhzd7gorew7fnd4k5pz3vhpgz524ehf6yuzuma";
    public static final String projectName = "int-terraform-aws-efs";
    public static final String repositoryName = "int-terraform-aws-efs";
    public static final String branchHeadHashB3 = "f1e1e49a8d5e1a3fa3a0edc2801ce21030e8fdc8";
    public static final String branchHeadHashMaster = "bc8618289aca9809b5073e0f32a4ea6d68dfcb0e";
    public static final String itemPath = "/tests/Jenkinsfile";
    public static final String jenkinsUrl = "http://8bc00b3e.ngrok.io/jenkins";
    public static final String scopePath = "/tests";
    public static final int pullRequestId = 8;

    @Test
    public void aTest0() throws Exception {
        ListAccountsRequest listAccountsRequest = new ListAccountsRequest(pat);
        OkHttp2Helper.INSTANCE.setDebugMode(true);
        Result<Accounts, Object> result = OkHttp2Helper.INSTANCE.executeRequest2(listAccountsRequest, Accounts.class, Object.class);
        Accounts accounts = result.getGoodValueOrNull();
//        assertThat(accounts.getCount(), is(1));
    }

    @Test
    public void aTest1() throws Exception {
        ListProjectsRequest listProjectsRequest = new ListProjectsRequest(collectionUrl, pat);
        OkHttp2Helper.INSTANCE.setDebugMode(true);
        Result<Projects, Object> result = OkHttp2Helper.INSTANCE.executeRequest2(listProjectsRequest, Projects.class, Object.class);
        Projects projects = result.getGoodValueOrNull();
        assertThat(projects.getCount(), is(2));
    }

    @Test
    public void aTest2() throws Exception {
        ListRepositoriesRequest listRepositoriesRequest = new ListRepositoriesRequest(collectionUrl, pat, projectName);
        OkHttp2Helper.INSTANCE.setDebugMode(true);
        Result<Repositories, Object> result = OkHttp2Helper.INSTANCE.executeRequest2(listRepositoriesRequest, Repositories.class, Object.class);
        Repositories repositories = result.getGoodValueOrNull();
        assertThat(repositories.getCount(), is(3));
    }

    @Test
    public void aTest3() throws Exception {
        ListRefsRequest listRefsRequest = new ListRefsRequest(collectionUrl, pat, projectName, repositoryName, "");
        OkHttp2Helper.INSTANCE.setDebugMode(true);
        Result<Refs, Object> result = OkHttp2Helper.INSTANCE.executeRequest2(listRefsRequest, Refs.class, Object.class);
        Refs refs = result.getGoodValueOrNull();
        if (refs != null) {
            System.out.println(refs.getCount());
            for (GitRef gitRef : refs.getValue()) {
                System.out.println(gitRef.getName() + "->" + gitRef.getObjectId() + "->" + gitRef.getUrl());
            }
        }
    }

    @Test
    public void aTest4() throws Exception {
        GetItemStreamRequest getItemStreamRequest = new GetItemStreamRequest(collectionUrl, pat, projectName, repositoryName, itemPath, branchHeadHashMaster, GitVersionType.commit);
        OkHttp2Helper.INSTANCE.setDebugMode(true);
        Result<InputStream, Object> result = OkHttp2Helper.INSTANCE.executeRequest2(getItemStreamRequest, InputStream.class, Object.class);
        InputStream inputStream = result.getGoodValueOrNull();
        assertNotNull(inputStream);
    }

    @Test
    public void aTest5() throws Exception {
        GetCommitRequest getCommitRequest = new GetCommitRequest(collectionUrl, pat, projectName, repositoryName, branchHeadHashB3);
        OkHttp2Helper.INSTANCE.setDebugMode(true);
        Result<GitCommit, Object> result = OkHttp2Helper.INSTANCE.executeRequest2(getCommitRequest, GitCommit.class, Object.class);
        GitCommit commit = result.getGoodValueOrNull();
        if (commit != null) {
            GitPushRef gitPushRef = commit.getPush();
            OffsetDateTime date = gitPushRef.getDate();
            System.out.println(date.toString());
        }
    }

    @Test
    public void aTest6() throws Exception {
        GetItemRequest getItemRequest = new GetItemRequest(collectionUrl, pat, projectName, repositoryName, itemPath, branchHeadHashB3, GitVersionType.commit);
        OkHttp2Helper.INSTANCE.setDebugMode(true);
        Result<GitItem, Object> result = OkHttp2Helper.INSTANCE.executeRequest2(getItemRequest, GitItem.class, Object.class);
        GitItem item = result.getGoodValueOrNull();
        if (item != null) {
            System.out.println(item.getPath());
            System.out.println(item.getUrl());
        }
    }

    @Test
    public void aTest7() throws Exception {
        ListItemsRequest listItemsRequest = new ListItemsRequest(collectionUrl, pat, projectName, repositoryName, "", branchHeadHashB3, GitVersionType.commit, VersionControlRecursionType.full);
        OkHttp2Helper.INSTANCE.setDebugMode(true);
        Result<Items, Object> result = OkHttp2Helper.INSTANCE.executeRequest2(listItemsRequest, Items.class, Object.class);
        Items items = result.getGoodValueOrNull();
        if (items != null) {
            for (GitItem item : items.getValue()) {
                System.out.println(item.getPath() + " -> " + (item.isFolder() ? "Folder" : "File"));
            }
        }
    }

    @Test
    public void aTest8() throws Exception {
        ListCommitsRequest listCommitsRequest = new ListCommitsRequest(collectionUrl, pat, projectName, repositoryName);
        OkHttp2Helper.INSTANCE.setDebugMode(true);
        Result<Commits, Object> result = OkHttp2Helper.INSTANCE.executeRequest2(listCommitsRequest, Commits.class, Object.class);
        Commits commits = result.getGoodValueOrNull();
        if (commits != null) {
            for (GitCommitRef commit : commits.getValue()) {
                System.out.println(commit.getCommitId() + " -> " + commit.getComment());
            }
        }
    }

    @Test
    public void aTest9() throws Exception {
        ListPullRequestsRequest listPullRequestsRequest = new ListPullRequestsRequest(collectionUrl, pat, projectName, repositoryName, PullRequestStatus.active, null);
        OkHttp2Helper.INSTANCE.setDebugMode(true);
        Result<PullRequests, Object> result = OkHttp2Helper.INSTANCE.executeRequest2(listPullRequestsRequest, PullRequests.class, Object.class);
        PullRequests pullRequests = result.getGoodValueOrNull();
        if (pullRequests != null) {
            for (GitPullRequest gitPullRequest : pullRequests.getValue()) {
                System.out.println(GsonProcessor.INSTANCE.instanceToJson(gitPullRequest));
                System.out.println(gitPullRequest.getPullRequestId() + " -> " + gitPullRequest.getLastMergeSourceCommit() + " -> " + gitPullRequest.getLastMergeTargetCommit() + " -> " + gitPullRequest.getLastMergeCommit());
            }
        }
    }

    @Test
    public void aTest10() throws Exception {
        ListPullRequestStatusesRequest listPullRequestStatusesRequest = new ListPullRequestStatusesRequest(collectionUrl, pat, projectName, repositoryName, 8);
        OkHttp2Helper.INSTANCE.setDebugMode(true);
        Result<PullRequestStatuses, Object> result = OkHttp2Helper.INSTANCE.executeRequest2(listPullRequestStatusesRequest, PullRequestStatuses.class, Object.class);
        PullRequestStatuses pullRequestStatuses = result.getGoodValueOrNull();
        if (pullRequestStatuses != null) {
            for (GitPullRequestStatus gitPullRequestStatus : pullRequestStatuses.getValue()) {
                System.out.println(GsonProcessor.INSTANCE.instanceToJson(gitPullRequestStatus));
            }
        }
    }

    @Test
    public void aTest11() throws Exception {
        GitPullRequestStatusForCreation gitPullRequestStatusForCreation = new GitPullRequestStatusForCreation(new GitStatusContext(jenkinsUrl, "asdfdsaf/pr-merge-12"), "PR-12#1: SUCCESS GOOD", GitStatusState.succeeded, "http://8bc00b3e.ngrok.io/jenkins/job/asdfdsaf/view/change-requests/job/PR-12/");
        CreatePullRequestStatusRequest createPullRequestStatusRequest = new CreatePullRequestStatusRequest(collectionUrl, pat, projectName, repositoryName, 12, gitPullRequestStatusForCreation);
        OkHttp2Helper.INSTANCE.setDebugMode(true);
        Result<GitPullRequestStatus, Object> result = OkHttp2Helper.INSTANCE.executeRequest2(createPullRequestStatusRequest, GitPullRequestStatus.class, Object.class);
        GitPullRequestStatus gitPullRequestStatus = result.getGoodValueOrNull();
        if (gitPullRequestStatus != null) {
            System.out.println(GsonProcessor.INSTANCE.instanceToJson(gitPullRequestStatus));
        }
    }

    @Test
    public void aTest12() throws Exception {
        ListPolicyConfigurationsRequest listPolicyConfigurationsRequest = new ListPolicyConfigurationsRequest(collectionUrl, pat, projectName);
        OkHttp2Helper.INSTANCE.setDebugMode(true);
        Result<PolicyConfigurations, Object> result = OkHttp2Helper.INSTANCE.executeRequest2(listPolicyConfigurationsRequest, PolicyConfigurations.class, Object.class);
        PolicyConfigurations policyConfigurations = result.getGoodValueOrNull();
        if (policyConfigurations != null) {
            System.out.println(policyConfigurations.getValue().size());
            for (PolicyConfiguration policyConfiguration : policyConfigurations.getValue()) {
                System.out.println(GsonProcessor.INSTANCE.instanceToJson(policyConfiguration));
            }
        }
    }
}
