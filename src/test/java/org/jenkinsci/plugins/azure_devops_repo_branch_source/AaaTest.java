package org.jenkinsci.plugins.azure_devops_repo_branch_source;

import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.*;
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.support.OkHttp2Helper;
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.support.Result;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class AaaTest {
//    @ClassRule
//    public static JenkinsRule j = new JenkinsRule();

    public static final String collectionUrl = "https://dev.azure.com/lukeshan";
    public static final String pat = "ltwwf6dxrqhvjalhzd7gorew7fnd4k5pz3vhpgz524ehf6yuzuma";
    public static final String projectName = "int-terraform-aws-efs";
    public static final String repository = "int-terraform-aws-efs";

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
        ListRefsRequest listRefsRequest = new ListRefsRequest(collectionUrl, pat, projectName, repository);
        OkHttp2Helper.INSTANCE.setDebugMode(true);
        Result<Refs, Object> result = OkHttp2Helper.INSTANCE.executeRequest2(listRefsRequest, Refs.class, Object.class);
        Refs refs = result.getGoodValueOrNull();
        assertThat(refs.getCount(), is(3));
    }
}
