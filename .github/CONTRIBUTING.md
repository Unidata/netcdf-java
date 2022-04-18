![netcdf-java icon](https://www.unidata.ucar.edu/images/logos/thredds_netcdf-150x150.png)

# Welcome contributors!

First off, thank you for your interest in contributing to the THREDDS project!
This repository contains the code for netCDF-Java.
The other projects held under the THREDDS umbrella are:
  * [THREDDS Data Server](https://github.com/unidata/tds) (TDS)
  * [Rosetta](https://github.com/unidata/rosetta)
  * [Siphon](https://github.com/unidata/siphon) (a python data access library, which includes an interface to the TDS).

Please review the [UCAR code of conduct](https://github.com/Unidata/.github/blob/develop/CODE_OF_CONDUCT.md) before submitting a pull request.
Once your have submitted a pull requests, you will be asked to digitally sign the Unidata Contributor License Agreement.
For more information about the CLA, please visit https://www.unidata.ucar.edu/blogs/developer/entry/contributor-license-agreement-for-unidata.

## Process Overview

* [GitHub Setup](#gh-setup)
  * [Join Github!](#gh-join)
  * [Fork the Unidata netCDF-java project](#gh-fork)
  * [Pull down local copy of the Unidata netCDF-java project](#gh-pull-ud-ncj)
  * [Add and pull down a local copy of your netCDF-java project fork](#gh-pull-personal-ncj)
* [Contribution workflow](#gh-contrib-workflow)
  * [Make sure you have the latest changes from Unidata netCDF-java repository](#gh-sync-ud)
  * [Make a new branch for your work and start hacking](#gh-branch)
  * [Follow these Style Guidelines](#gh-style-guidelines)
  * [Clean up your git commit history](#gh-history-cleanup)
  * [Push changes to your fork to use for the pull request](#gh-final-commit-for-pr)
  * [Make the pull request](#gh-pr)
* [Now what?](#gh-now-what)

## <a name="gh-setup"></a>GitHub Setup

### <a name="gh-join"></a>Join Github!
To get started contributing to a THREDDS project, the first thing you should do is [sign up for a free account on GitHub](https://github.com/join).

### <a name="gh-fork"></a>Fork the Unidata netcdf-java project
Once you have an account, go ahead and [fork](https://github.com/unidata/netcdf-java#fork-destination-box) the netCDF-java project.
By forking the project, you will have a complete copy of the netCDF-java project, history and all, under your personal account.
This will allow you to make pull requests against the Unidata netCDF-java repository, which is the primary mechanism used to add new code to the project (even Unidata developers do this!).

### <a name="gh-pull-ud-ncj"></a>Pull down local copy of the Unidata netCDF-java project
After cloning the Unidata repository, you can pull down the source code to your local machine by using git:

`git clone --origin unidata git@github.com:Unidata/netcdf-java.git` (for ssh)

or

`git clone --origin unidata https://github.com/Unidata/netcdf-java.git` (for http)

Note that these commands reference the Unidata repository.

Normally in git, the remote repository you clone from is automatically named `origin`.
To help with any confusion when making pull requests, this commands above rename the remote repository to `unidata`.

### <a name="gh-join"></a>Add and pull down a local copy of your netCDF-java project fork

Next, move into the source directory git has created, and add your personal fork of the netCDF-java code as a remote"

`git clone --origin me git@github.com:<my-github-user-name>/netcdf-java.git` (for ssh)

or

`git clone --origin me https://github.com/,my-github-user-name>/netcdf-java.git` (for https)

Now you are all set!

## <a name="#gh-contrib-workflow"></a>Contribution workflow

### <a name="#gh-sync-ud"></a> Make sure you have the latest changes from Unidata netCDF-java repository
First, make sure you have the most recent changes to the netCDF-java code by using git pull:

`git pull unidata develop`

All work on netcdf-java is should branch from the `develop` branch.
Contributions will be backported to other versions as needed.

### <a name="#gh-branch"></a>Make a new branch for your work and start hacking
Next, make a new branch where you will actually do the hacking:

`git checkout -b mywork`

As of this point, the branch `mywork` is local.
To make this branch part of your personal GitHub Remote repository, use the following command:

`git push -u me mywork`

Now git (on your local machine) is setup to a point where you can start hacking on the code and commit changes to your personal GitHub repository.

At any point, you may add commits to your local copy of the repository by using:

`git commit`

If you would like these changes to be stored on your personal remote repository, simply use:

`git push me mywork`

Once you are satisfied with your work, there is one last step to complete before submitting the pull request - clean up the history.

### <a name="#gh-style-guidelines"></a>Follow these Style Guidelines

#### Java, Groovy, and Gradle
We are using the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html) for Java, Groovy, and Gradle, with these exceptions and augmentations:

1. The recommended line width is 120, not 100.
   Modern screens are much wider than tall, so having wider lines allows more code to fit on a screen.

2. As a rule, don't add empty javadoc that has no information that can't be seen directly in the code.
   For example, do not do this:

   ~~~java
     /**
      * _more_
      *
      * @param nds _more_
      * @param v   _more_
      * @return _more_
      * @throws IOException
      */
     protected Foo makeFoo(NetcdfDataset nds, VariableSimpleIF v) throws IOException
   ~~~

   Better to not have any javadoc then to have empty javadoc.
   Of course, best is to put actual, useful comments to help others understand your API.

To assist in following the style, we provide both IntelliJ and Eclipse style files.
These files can be found in the under the `project-files/code-styles/` directory of the repository.
If you are not using an IDE, you can check the style using:

~~~bash
./gradlew spotlessCheck
~~~

and you can apply the style using:

~~~bash
./gradlew spotlessApply
~~~

It can be easy to forget to run this command before pushing your changes to github.
For that, we have created git pre-commit hook scripts for you to use.
The pre-commit hook will run the spotlessApply task before the commit is made, ensuring that you have the proper code format.
To install the pre-commit hook in a *nix environment (linux, *BSD, MacOS, Cygwin, etc.), copy the file `hooks/pre-commit-nix` to `.git/hooks/pre-commit` and make it executable.
On windows, copy the file `hooks/pre-commit-windows` to `.git/hooks/pre-commit`.

#### Markdown

When writing markdown, we use one line per sentence.
At this time, markdown is not inspected by spotless.

### <a name="#gh-history-cleanup"></a>Clean up your git commit history

Commit history can often be full of temporary commit messages, and/or commits with code changes that ultimately didn't make the final cut.

To clean up your history, use the `git rebase -i` command, which will open an editor:

~~~bash
sarms@flip: [mywork]$ git rebase -i
pick 083508e first commit of my really cool feature or bug fix!
pick 9bcba01 Oops missed this one thing. This commit fixes that.

# Rebase 083508e..9bcba01 onto 083508e (2 command(s))
#
# Commands:
# p, pick = use commit
# r, reword = use commit, but edit the commit message
# e, edit = use commit, but stop for amending
# s, squash = use commit, but meld into previous commit
# f, fixup = like "squash", but discard this commit's log message
# x, exec = run command (the rest of the line) using shell
# d, drop = remove commit
#
# These lines can be re-ordered; they are executed from top to bottom.
#
# If you remove a line here THAT COMMIT WILL BE LOST.
#
# However, if you remove everything, the rebase will be aborted.
#
# Note that empty commits are commented out
~~~

Based on my commit messages, you can see that commit `9bcba01` fixed a mistake from my first commit.

It would be nice to `squash` those changes into the first commit, so that the official history does not show my mistake..uhhh...this extra commit.

To do so, edit the text to change the second commits `pick` to `squash`:

~~~bash
pick 083508e first commit of my really cool feature or bug fix!
squash 9bcba01 Oops missed this one thing. This commit fixes that.

# Rebase 083508e..9bcba01 onto 083508e (2 command(s))
#
# Commands:
# p, pick = use commit
# r, reword = use commit, but edit the commit message
# e, edit = use commit, but stop for amending
# s, squash = use commit, but meld into previous commit
# f, fixup = like "squash", but discard this commit's log message
# x, exec = run command (the rest of the line) using shell
# d, drop = remove commit
#
# These lines can be re-ordered; they are executed from top to bottom.
#
# If you remove a line here THAT COMMIT WILL BE LOST.
#
# However, if you remove everything, the rebase will be aborted.
#
# Note that empty commits are commented out
~~~

Once you have marked the commits to be squashed and exited the edit, you will prompted to change the commit message for the new, squashed, mega commit:

~~~bash
# This is a combination of 2 commits.
# The first commit's message is:

first commit of my really cool feature or bug fix!

# This is the 2nd commit message:

Oops missed this one thing. This commit fixes that.

#Please enter the commit message for your changes. Lines starting
# with '#' will be ignored, and an empty message aborts the commit.
#
# Date:      Thu Oct 15 09:59:23 2015 -0600
#
# interactive rebase in progress; onto 083508e
# Last commands done (2 commands done):
#    pick 09134d5 first commit of my really cool feature or bug fix!
#    squash 9bcba01 Oops missed this one thing. This commit fixes that.
# No commands remaining.
# You are currently editing a commit while rebasing branch 'mywork' on '0835    08e'.
#
# Changes to be committed:
...
~~~

Edit the two commit messages into a single message that describes the overall change:

~~~

Once you have and exit, you will have a change to change the commit message for the new, squashed, mega commit:

~~~bash

Really cool feature or bug fix. Addresses the github issue Unidata/netcdf-java#1

#Please enter the commit message for your changes. Lines starting
# with '#' will be ignored, and an empty message aborts the commit.
#
# Date:      Thu Oct 15 09:59:23 2015 -0600
#
# interactive rebase in progress; onto 083508e
# Last commands done (2 commands done):
#    pick 09134d5 first commit of my really cool feature or bug fix!
#    squash 9bcba01 Oops missed this one thing. This commit fixes that.
# No commands remaining.
# You are currently editing a commit while rebasing branch 'mywork' on '0835    08e'.
#
# Changes to be committed:
...
~~~

Now, when you look at your git commit logs, you will see:

~~~bash
commit 805b4723c4a2cbbed240354332cd7af57559a1b9
Author: Sean Arms <67096+lesserwhirls@users.noreply.github.com>
Date:   Thu Oct 15 09:59:23 2015 -0600

    Really cool feature or bug fix. Addresses the github issue Unidata/netcdf-java#1

~~~

Note that the commit contains the text `Unidata/netcdf-java#1`.
This is a cool github trick that will allow you to reference GitHub issues within your commit messages.
When viewed on github.com, this will be turned into a hyperlink to the issue.
While not every contribution will address an issue, please use this feature if your contribution does!

### <a name="#gh-final-commit-for-pr"></a>Push changes to your fork to use for the pull request
Now that you have cleaned up the history, you will need to make a final push to your personal GitHub repository.
However, the rebase has changed the history of your local branch, which means you will need to use the `--force` flag in your push:

`git push --force me mywork`

### <a name="#gh-pr"></a>Make the pull request
Finally, go to your personal remote repository on github.com and switch to your `mywork` branch.
Once you are on your work branch, you will see a button that says "Pull request", which will allow you to make a pull request.

The github pull request page will allow you to select which repository and branch you would like to submit the pull request to (the `base fork`, which should be `Unidata/netcdf-java`, and `base`, which should be `develop`), as well as the `head fork` and `compare` (which should be `<github-user-name/netcdf-java>` and `mywork`, respectively).
Once this is setup, you can make the pull request.

## <a name="#gh-now-what"></a>Now what?

The Unidata netCDF-java project uses GitHub Actions for automated testing for all pull requests.
The status of the tests can be seen on the pull request page.
For example, visit [Unidata/netcdf-java#355](https://github.com/Unidata/netcdf-java/pull/355) and select the [Checks](https://github.com/Unidata/netcdf-java/pull/355/checks) tab along the top.
GitHub Actions check the following:
* build and test netCDF-Java using AdoptOpenJDK 8, Zulu JDK 8 (*required*)
* build the documentation, if any documentation changes were made in the PR. (*required*)
* code style check using `spotlessCheck`
* build and testing the THREDDS Data Server against the PR

Pull requests whose changes cause one or more of the first three checks to fail are generally not merged.
One of the Unidata netCDF-java team members will work with you to make sure your work is ready for merging once the tests have passed.
Any changes to your pull request will trigger the GitHub Actions to re-run.

If your pull request addresses a bug, we kindly ask that you include a test in your pull request.
Learn more about testing within netCDF-Java on our [wiki](https://github.com/Unidata/netcdf-java/wiki/Testing)
If you have test data that are larger than a few hundred kB, please let us know.
If you cannot reduce the size of the test file, we can add it to our [integration test datasets](https://github.com/unidata/thredds-test-data), as long as we have permission to redistribute the file.
If you do not know how to write tests in Java, we will be more than happy to work with you!
