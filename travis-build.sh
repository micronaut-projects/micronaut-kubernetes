#!/bin/bash
set -e
set -x
EXIT_STATUS=0

CLIENT_POD="$(kubectl get pods | grep "example-client" | awk 'FNR <= 1 { print $1 }')"
SERVICE_POD_1="$(kubectl get pods | grep "example-service" | awk 'FNR <= 1 { print $1 }')"
SERVICE_POD_2="$(kubectl get pods | grep "example-service" | awk 'FNR > 1 { print $1 }')"

function showLogs {
    echo "Build failed! Displaying pod logs"
    echo "Client pod logs:"
    kubectl logs $CLIENT_POD

    echo "Service pod #1 logs:"
    kubectl logs $SERVICE_POD_1

    echo "Service pod #2 logs:"
    kubectl logs $SERVICE_POD_2
}

if [ "${TRAVIS_JDK_VERSION}" == "openjdk11" ] ; then
    echo "Check for branch $TRAVIS_BRANCH JDK: $TRAVIS_JDK_VERSION"
    ./gradlew testClasses --no-daemon --stacktrace|| EXIT_STATUS=$?

    if [ $EXIT_STATUS -ne 0 ]; then
        showLogs
       exit $EXIT_STATUS
    fi

    ./gradlew --stop
    ./gradlew check --no-daemon --stacktrace || EXIT_STATUS=$?

    if [ $EXIT_STATUS -ne 0 ]; then
        showLogs
       exit $EXIT_STATUS
    fi

    ./gradlew --stop
    ./gradlew assemble --no-daemon || EXIT_STATUS=$?

    if [ $EXIT_STATUS -ne 0 ]; then
        showLogs
    fi

    exit $EXIT_STATUS
fi

if [[ $EXIT_STATUS -eq 0 ]]; then
    if [[ -n $TRAVIS_TAG ]]; then
        echo "Skipping Tests to Publish Release"
        ./gradlew pTML assemble --no-daemon || EXIT_STATUS=$?
    else
        ./gradlew --stop
        ./gradlew testClasses --no-daemon --stacktrace || EXIT_STATUS=$?

        if [ $EXIT_STATUS -ne 0 ]; then
            showLogs
            exit $EXIT_STATUS
        fi

        ./gradlew --stop
        ./gradlew check --no-daemon --stacktrace || EXIT_STATUS=$?

        if [ $EXIT_STATUS -ne 0 ]; then
            showLogs
        fi
    fi
fi

if [[ $EXIT_STATUS -eq 0 ]]; then
    echo "Publishing archives for branch $TRAVIS_BRANCH"

    if [[ -n $TRAVIS_TAG ]] || [[ $TRAVIS_BRANCH =~ ^master$ && $TRAVIS_PULL_REQUEST == 'false' ]]; then
        echo "Publishing archives"
        ./gradlew --stop

        if [[ -n $TRAVIS_TAG ]]; then
            ./gradlew bintrayUpload --no-daemon --stacktrace || EXIT_STATUS=$?
            if [ $EXIT_STATUS -ne 0 ]; then
                exit $EXIT_STATUS
            fi

            ./gradlew synchronizeWithMavenCentral --no-daemon || EXIT_STATUS=$?

            if [ $EXIT_STATUS -ne 0 ]; then
                exit $EXIT_STATUS
            fi
        else
            ./gradlew publish --no-daemon --stacktrace || EXIT_STATUS=$?

            if [ $EXIT_STATUS -ne 0 ]; then
                exit $EXIT_STATUS
            fi
        fi

        git config --global user.name "$GIT_NAME"
        git config --global user.email "$GIT_EMAIL"
        git config --global credential.helper "store --file=~/.git-credentials"
        echo "https://$GH_TOKEN:@github.com" > ~/.git-credentials

        ./gradlew -x javaDocAtReplacement --console=plain --no-daemon docs || EXIT_STATUS=$?

        if [ $EXIT_STATUS -ne 0 ]; then
            exit $EXIT_STATUS
        fi

        git clone https://${GH_TOKEN}@github.com/micronaut-projects/micronaut-kubernetes.git -b gh-pages gh-pages --single-branch > /dev/null

        cd gh-pages

        # If this is the master branch then update the snapshot
        if [[ $TRAVIS_BRANCH =~ ^master|[12]\..\.x$ ]]; then
            mkdir -p snapshot
            cp -r ../build/docs/. ./snapshot/
            git add snapshot/*
        fi

        # If there is a tag present then this becomes the latest
        if [[ -n $TRAVIS_TAG ]]; then
            mkdir -p latest
            cp -r ../build/docs/. ./latest/
            git add latest/*

            version="$TRAVIS_TAG"
            version=${version:1}
            majorVersion=${version:0:4}
            majorVersion="${majorVersion}x"

            mkdir -p "$version"
            cp -r ../build/docs/. "./$version/"
            git add "$version/*"

            mkdir -p "$majorVersion"
            cp -r ../build/docs/. "./$majorVersion/"
            git add "$majorVersion/*"

        fi

        git commit -a -m "Updating docs for Travis build: https://travis-ci.org/$TRAVIS_REPO_SLUG/builds/$TRAVIS_BUILD_ID" && {
          git push origin HEAD || true
        }
        cd ..

        rm -rf gh-pages
    fi
fi

exit $EXIT_STATUS
