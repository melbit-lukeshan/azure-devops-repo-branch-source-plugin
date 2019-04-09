Azure DevOps Repo Branch Source plugin for Jenkins
==================================================

###We try to mimic GitHub Branch Source plugin's functionality but with Azure DevOps as the repository provider.

####This project is a mix of [GitHub Branch Source plugin] for Jenkins and [Team Foundation Server plugin] for Jenkins.

####Source come from GitHub Branch Source plugin:
* Jenkins job configuration
* Branch, Tag and Pull Request scan

####Source come from Team Foundation Server plugin:
* Web hooks handling

####Source we add:
* Azure DevOps api modeling
* Azure DevOps api requesting utils

[GitHub Branch Source plugin]: https://github.com/jenkinsci/github-branch-source-plugin
[Team Foundation Server plugin]: https://github.com/jenkinsci/tfs-plugin