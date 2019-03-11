package org.jenkinsci.plugins.azure_devops_repo_branch_source;

import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.ListProjectsRequest;
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.Projects;
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.support.OkHttp3Helper;
import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.support.Result;
import org.junit.Test;

public class AaaTest {
//    @ClassRule
//    public static JenkinsRule j = new JenkinsRule();

    @Test
    public void aTest() throws Exception {
        ListProjectsRequest listProjectsRequest = new ListProjectsRequest("zus3crtwlto6asnshsuhhraqhqmkiguzn5ocxirp33purukjy6eq", "lukeshan");
        OkHttp3Helper.INSTANCE.setDebugMode(true);
        Result<Projects, Object> result = OkHttp3Helper.INSTANCE.executeRequest2(listProjectsRequest, Projects.class, Object.class);
        //assertThat(result.getGoodValueOrNull(), is((Projects) null));
    }
}
