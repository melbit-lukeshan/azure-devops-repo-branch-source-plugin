package org.jenkinsci.plugins.azure_devops_repo_branch_source;

import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.ListProjectsRequest;
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.ListRepositoriesRequest;
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.Projects;
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.Repositories;
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.support.OkHttp2Helper;
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.support.Result;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class AaaTest {
//    @ClassRule
//    public static JenkinsRule j = new JenkinsRule();

    @Test
    public void aTest1() throws Exception {
        ListProjectsRequest listProjectsRequest =
                new ListProjectsRequest("https://dev.azure.com/lukeshan",
                        "ltwwf6dxrqhvjalhzd7gorew7fnd4k5pz3vhpgz524ehf6yuzuma");
        OkHttp2Helper.INSTANCE.setDebugMode(true);
        Result<Projects, Object> result = OkHttp2Helper.INSTANCE.executeRequest2(listProjectsRequest, Projects.class, Object.class);
        Projects projects = result.getGoodValueOrNull();
        assertThat(projects.getCount(), is(2));
    }

    @Test
    public void aTest2() throws Exception {
        ListRepositoriesRequest listRepositoriesRequest =
                new ListRepositoriesRequest("https://dev.azure.com/lukeshan",
                        "ltwwf6dxrqhvjalhzd7gorew7fnd4k5pz3vhpgz524ehf6yuzuma",
                        "int-terraform-aws-efs");
        OkHttp2Helper.INSTANCE.setDebugMode(true);
        Result<Repositories, Object> result = OkHttp2Helper.INSTANCE.executeRequest2(listRepositoriesRequest, Repositories.class, Object.class);
        Repositories repositories = result.getGoodValueOrNull();
        assertThat(repositories.getCount(), is(3));
    }
}
