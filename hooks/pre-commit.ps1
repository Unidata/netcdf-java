# pre-commit hook based on that used by the Uber project NullAway (ported to PowerShell by sarms)
# https://github.com/uber/NullAway/blob/a5cdecba0e33eed5c8dd86f43bc2cc6408e6fd37/config/hooks/pre-commit

$REPO_ROOT_DIR = git rev-parse --show-toplevel

$FILES = git diff --cached --name-only --diff-filter=ACMR | Select-String -Pattern "\.java$"

if (-Not ($FILES -eq $null)) {
  'Check and apply (if needed) the code style to these staged java files:'
  $FILES | foreach {
   write-output "    $_"
  }
  $COMMA_SEP_FILES = ($FILES -join ",")
  ./gradlew spotlessApply -Pfiles=""$COMMA_SEP_FILES"" 2>&1 | Out-Null
  write-output "...the code is spotless! :-)"
  git add $FILES
}
