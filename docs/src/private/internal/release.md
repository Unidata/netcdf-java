## Making a release for netCDF-java using Gradle

First, ensure the last run of master on Jenkins looks good:
 * no open CVEs
 * tests look good

`${releaseVersion}` refers to the full, 3-part version, e.g. `5.0.0`.

`origin` refers to the remote Unidata GitHub repository (i.e. https://github.com/Unidata/netcdf-java.git).
If you have renamed the Unidata remote repository, make sure to use that name in place of `origin`.
If you are unsure, check `git remote -v`.

**Note 1**: If you are releasing a new patch version to a previous release due to security fixes, it's assumed that things look good on Jenkins and you are up-to-date.
If so, you can skip to step 4.

1. Make sure userguide is up-to-date:
   - `docs/src/public/userguide/_config.yml`
       - Update the docset_version to the release version (major.minor).
   - `docs/src/public/userguide/pages/netcdfJava/UpgradeTo50.md`
       - make sure changes for minor version are documented.
   - Note: this is a safety check.
     This should have been done as soon as the version in `build.gradle` was changed to a new major or minor version.

1. Ensure that there are no uncommitted changes, e.g.
   - `git checkout master`
   - `git status`

1. Pull all of the latest changes from upstream.
   - `git pull`

1. Create a new branch for the release and switch to it.
   Branch from an up-to-date `Unidata/netcdf-java master` or appropriate maintenance branch (e.g. `Unidata/netcdf-java 5.3-maint`).
   - `git checkout -b ${releaseVersion}`

1. In `/build.gradle`, update the project's version for the release.
   - Likely, this means removing the `-SNAPSHOT` prefix, e.g. `5.0.0-SNAPSHOT` to `5.0.0`.
   - change the `status` property from 'development' to 'release'

1. Commit the changes you've made.
    - At the very least, `project.version` and `status` in the root build script should have been modified.
    - `git add ...`
    - `git commit -m "Release ${releaseVersion}"`

1. Push branch to Unidata/netcdf-java
    - `git push -u origin ${releaseVersion}`

1. Log into Jenkins and perform release
    - Go to netcdf-java-release project
    - Edit the settings to point to the ${releaseVersion} branch on GitHub
    - Run project
    - Check artifacts at https://artifacts.unidata.ucar.edu/#browse/browse:unidata-all
    - As long as we don't make it to the artifact publishing step, we can always fix any issues by squashing new commits and pushing to the branch on Unidata/netcdf-java

1. Update Unidata download page
    - check https://www.unidata.ucar.edu/downloads/netcdf-java/
      * Edit (on machine www) `/content/downloads/netcdf-java/index.html` as needed. for example in `vim`:
      ```bash
      :%s/5.2.0/5.3.2/gc
      ```
      This will update the links to the release artifacts.

1. Prepare for next round of development.
    - Update the project version. Increment it and add the "-SNAPSHOT" suffix.
      * For example, `if ${releaseVersion} == "5.0.0"`, the next version will be "5.0.1-SNAPSHOT".
      * Change the `status` property from 'release' to 'development'
    - Commit the change.
      * `git add ...`
      * `git commit -m "Begin work on 5.0.1-SNAPSHOT"`
    - If moving directly to new minor version (i.e. `5.0.0` to `5.1.0`), revisit step 1 and update with new minor version.

1. Push the new commits upstream.
    - `git push origin ${releaseVersion}`

1. Create a pull request on GitHub and wait for it to be merged.
    - It should pull your changes on `Unidata/${releaseVersion}` into `Unidata/netcdf-java master` (or the appropriate maintenance branch, e.g. `Unidata/netcdf-java 5.3-maint`).
    - Alternatively, merge it yourself. As long as the changeset is small and non-controversial, nobody will care.

1. Once merged, pull down the latest changes from master or appropriate maintenance branch (e.g. `Unidata/netcdf-java 5.3-maint`).
   You can also delete the local release branch.
    - `git checkout master`
    - `git pull`
    - `git branch -d ${releaseVersion}`

1. In the git log of master (or appropriate maintenance branch), find the "Release ${releaseVersion}" commit and tag it with the version number.
    - `git log`
    - `git tag v${releaseVersion} <commit-id>`
    - You can't create this tag earlier because when our PR was merged above, GitHub rebased our original
      commits, creating brand new commits in the process. We want to apply the tag to the new commit,
      because it will actually be part of `master`'s history.

1. Push the release tag upstream.
    -  `git push origin v${releaseVersion}`

1. In the GitHub interface, delete the branch used to hold the release PR. Let's keep things tidy.
    - If we need to do a security update on a previous minor release, we'll create a new branch from the committed tag and work from that, ultimately following the instructions above when we're done.

1. Create a release on GitHub using the tag you just pushed.
    - Example: https://github.com/Unidata/netcdf-java/releases/tag/v5.1.0
    - To help create the changelog, examine the pull requests on GitHub. For example, this URL shows all PRs that
      have been merged into `master` since 2019-09-12:
      
      https://github.com/Unidata/netcdf-java/pulls?q=base%3Amaster+merged%3A%3E%3D2019-09-12
      
      and this URL shows the commits between two tags (`v5.0.0` and `v5.1.0`):
      
      https://github.com/Unidata/netcdf-java/compare/v5.0.0...v5.1.0

1. Make blog post for the release.
    - Example: https://www.unidata.ucar.edu/blogs/news/entry/netcdf-java-library-version-51
    - Best to leave it relatively short and just link to the GitHub release.

1. Make a release announcement to the mailing lists: netcdf-java@unidata.ucar.edu
    - Example: https://www.unidata.ucar.edu/mailing_lists/archives/netcdf-java/2019/msg00013.html
    - Best to leave it relatively short and just link to the GitHub release.

**Note 2**: In the future, we could be performing even more, if not all, of these steps from Jenkins.