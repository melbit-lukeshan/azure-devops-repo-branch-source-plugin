package org.jenkinsci.plugins.azure_devops_repo_branch_source;

import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.*;
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
    public static final String branchHeadHash = "db7a5d4e6139e341534a6a0bebdd86ab6248bc10";
    public static final String readmeUrl = "https://dev.azure.com/lukeshan/cd168403-6d20-4056-914b-cab7f07d9598/_apis/git/repositories/5e438059-083c-4db7-ab73-54d838b5d20d/Items?path=%2FREADME.md&versionDescriptor%5BversionOptions%5D=0&versionDescriptor%5BversionType%5D=0&versionDescriptor%5Bversion%5D=master&download=true&resolveLfs=true&%24format=octetStream&api-version=5.0-preview.1";
    public static final String itemPath = "/README.md";
    public static final String scopePath = "/tests";

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
        ListRefsRequest listRefsRequest = new ListRefsRequest(collectionUrl, pat, projectName, repositoryName, "heads/b3");
        OkHttp2Helper.INSTANCE.setDebugMode(true);
        Result<Refs, Object> result = OkHttp2Helper.INSTANCE.executeRequest2(listRefsRequest, Refs.class, Object.class);
        Refs refs = result.getGoodValueOrNull();
        if (refs != null) {
            System.out.println(refs.getCount());
            for (GitRef gitRef : refs.getValue()) {
                System.out.println(gitRef.getName() + "->" + gitRef.getObjectId() + "->" + gitRef.getUrl());
            }
        }
        assertThat(refs.getCount(), is(1));
    }

    @Test
    public void aTest4() throws Exception {
        GetItemStreamRequest getItemStreamRequest = new GetItemStreamRequest(collectionUrl, pat, readmeUrl);
        OkHttp2Helper.INSTANCE.setDebugMode(true);
        Result<InputStream, Object> result = OkHttp2Helper.INSTANCE.executeRequest2(getItemStreamRequest, InputStream.class, Object.class);
        InputStream inputStream = result.getGoodValueOrNull();
        assertNotNull(inputStream);
    }

    @Test
    public void aTest5() throws Exception {
        GetCommitRequest getCommitRequest = new GetCommitRequest(collectionUrl, pat, projectName, repositoryName, branchHeadHash);
        OkHttp2Helper.INSTANCE.setDebugMode(true);
        Result<GitCommit, Object> result = OkHttp2Helper.INSTANCE.executeRequest2(getCommitRequest, GitCommit.class, Object.class);
        GitCommit commit = result.getGoodValueOrNull();
        if (commit != null) {
            GitPushRef gitPushRef = commit.getPush();
            if (gitPushRef != null) {
                OffsetDateTime date = gitPushRef.getDate();
                if (date != null) {
                    System.out.println(date.toString());
                } else {
                    System.out.println("date is null");
                }
            } else {
                System.out.println("gitPushRef is null");
            }
        }
    }

    @Test
    public void aTest6() throws Exception {
        GetItemRequest getItemRequest = new GetItemRequest(collectionUrl, pat, projectName, repositoryName, itemPath);
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
        ListItemsRequest listItemsRequest = new ListItemsRequest(collectionUrl, pat, projectName, repositoryName, "", VersionControlRecursionType.full);
        OkHttp2Helper.INSTANCE.setDebugMode(true);
        Result<Items, Object> result = OkHttp2Helper.INSTANCE.executeRequest2(listItemsRequest, Items.class, Object.class);
        Items items = result.getGoodValueOrNull();
        if (items != null) {
            for (GitItem item : items.getValue()) {
                System.out.println(item.getPath() + " -> " + (item.isFolder() ? "Folder" : "File"));
            }
        }
    }
}
