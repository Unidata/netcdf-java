## Making a release for CDM/TDS using Gradle

First, ensure the last run of master on Jenkins looks good:
 * no open CVEs
 * tests look good

`${releaseVersion}` refers to the full, 3-part version, e.g. `5.0.0`.

`origin` refers to the remote Unidata github repository (i.e. https://github.com/Unidata/netcdf-java.git).
If you have renamed the Unidata remote repository, make sure to use that name in place of `origin`.
If you are unsure, check `git remote -v`.

**Note 1**: If you are releasing a new patch version to a previous release due to security fixes, it's assumed that things look good on Jenkins and you are up-to-date.
If so, you can skip to step 4.

1. Ensure that there are no uncommitted changes, e.g.
   - `git checkout master`
   - `git status`

1. Pull all of the latest changes from upstream.
   - `git pull`

1. Create a new branch for the release and switch to it.
   - `git checkout -b ${releaseVersion}`

1. In `/build.gradle`, update the project's version for the release.
   - Likely, this means removing the `-SNAPSHOT` prefix, e.g. `5.0.0-SNAPSHOT` to `5.0.0`.
   - change the `status` property from 'development' to 'release'

1. Commit the changes you've made.
    - At the very least, `project.version` and `status` in the root build script should have been modified.
    - `git add ...`
    - `git commit -m "Release ${releaseVersion}"`

1. Push branch to Unidata/netcdf-java
    - `gith push -u origin ${releaseVersion}`

1. Log into Jenkins and perform release
    - Go to netcdf-java-release project
    - Edit the settings to point to the ${releaseVersion} branch on github
    - Run project
    - Check artifacts at https://artifacts.unidata.ucar.edu/#browse/browse/components:unidata-releases
    - As long as we don't make it to the artifact publishing step, we can always fix any issues by squashing new commits and pushing to the branch on Unidata/netcdf-java

1. Update Unidata download page
    - check http://www.unidata.ucar.edu/downloads/netcdf/netcdf-java-4/index.jsp
      * modify `www:/content/downloads/netcdf/netcdf-java-4/toc.xml` as needed

1. Prepare for next round of development.
    - Update the project version. Increment it and add the "-SNAPSHOT" suffix.
      * For example, `if ${releaseVersion} == "5.0.0"`, the next version will be "5.0.1-SNAPSHOT".
      * Change the `status` property from 'release' to 'development'
    - Commit the change.
      * `git add ...`
      * `git commit -m "Begin work on 5.0.1-SNAPSHOT"`

1. Push the new commits upstream.
    - `git push origin ${releaseVersion}`

1. Create a pull request on GitHub and wait for it to be merged.
    - It should pull your changes on `Unidata/${releaseVersion}` into `Unidata/netcdf-java master`.
    - Alternatively, merge it yourself. As long as the changeset is small and non-controversial, nobody will care.

1. Once merged, pull down the latest changes from master. You can also delete the local release branch.
    - `git checkout master`
    - `git pull`
    - `git branch -d ${releaseVersion}`

1. In the git log of master, find the "Release ${releaseVersion}" commit and tag it with the version number.
    - `git log`
    - `git tag v${releaseVersion} <commit-id>`
        * `HEAD~1` is usually the right commit, so you can probably do `git tag v${releaseVersion} HEAD~1`
    - You can't create this tag earlier because when our PR was merged above, GitHub rebased our original
      commits, creating brand new commits in the process. We want to apply the tag to the new commit,
      because it will actually be part of `master`'s history.

1. Push the release tag upstream.
    -  `git push origin v${releaseVersion}`

1. In the github interface, delete the branch used to hold the release PR. Let's keep things tidy.
    - If we need to do a security update on a previous minor release, we'll create a new branch from the committed tag and work from that, ultimately following the instructions above when we're done.

1. Create a release on GitHub using the tag you just pushed.
    - Example: https://github.com/Unidata/thredds/releases/tag/v5.0.0
    - To help create the changelog, examine the pull requests on GitHub. For example, this URL shows all PRs that
      have been merged into `master` since 2016-02-12:
      https://github.com/Unidata/thredds/pulls?q=base%3Amaster+merged%3A%3E%3D2016-02-12

1. Make blog post for the release.
    - Example: http://www.unidata.ucar.edu/blogs/news/entry/netcdf-java-library-and-tds4
    - Best to leave it relatively short and just link to the GitHub release.

1. Make a release announcement to the mailing lists: netcdf-java@unidata.ucar.edu and thredds@unidata.ucar.edu
    - Example: http://www.unidata.ucar.edu/mailing_lists/archives/netcdf-java/2017/msg00000.html
    - Best to leave it relatively short and just link to the GitHub release.

**Note 2**: In the future, we should be performing many (all?) of these steps from Jenkins, not our local machine.
