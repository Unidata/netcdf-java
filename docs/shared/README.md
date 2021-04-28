# How to use the shared directory

The directory structure mimics the structure of a any of the jekyll docsets.
If you would like a file to be shared between the various docsets, two things need to happen:

1. place the file in the appropriate location under `shared/src/site/`
2. add the extension `.golden` to the file.

Only files that end in `.golden` will be shared.
The shared files will be copied prior to running the jekyll build commands so that they are in place when needed.
The files will be removed as part of the `clean` task.
They are also added to `.gitignore`, so they should not be accidentally checked-in in the future.
