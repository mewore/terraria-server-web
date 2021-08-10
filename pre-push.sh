#!/bin/bash

REMOTE=$1
#REPO_URL=$2

printError() {
  message=$1
  echo "========================================="
  echo "==> ERROR: ${message}"
  echo "========================================="
}

if [ -n "$(git diff --name-only)" ]; then
  printError "There are uncommitted changes! Commit or stash everything before trying to push."
  exit 1
fi

files_to_push=$(git diff --name-only "${REMOTE}")

if echo "${files_to_push}" | grep -q '.gradle'; then
  echo "Performing 'clean'..."
  for subProject in 'backend' 'frontend'; do
    ./gradlew "${subProject}:clean" || exit 2
  done
  ./gradlew clean || exit 2
else
  echo "There are no changed Gradle project files so the 'clean' task will not be performed."
fi

# See if the full build with test+lint and packaging into a jar works
./gradlew jar -PuseCheckerFramework
result=$?

compareNumbers() {
  awk -v first="$1" -v second="$2" 'BEGIN {printf first < second ? "<" : ">="}'
}

FRONTEND_COVERAGE_TARGET=90
grep -Eo -m 4 '[0-9.]+%' ./frontend/coverage/terraria-server-web/index.html | while read match; do
  coverage=`echo "${match}" | sed 's/%//'`
  comparison_result=`compareNumbers "${coverage}" "${FRONTEND_COVERAGE_TARGET}"`
  if [ "${comparison_result}" = '<' ]; then
    printError "Frontend coverage target has not been met: ${coverage}% < ${FRONTEND_COVERAGE_TARGET}%"
    exit 1
  fi
done

exit $result
