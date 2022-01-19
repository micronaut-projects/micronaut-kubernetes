# Micronaut Kubernetes

[![Maven Central](https://img.shields.io/maven-central/v/io.micronaut.kubernetes/micronaut-kubernetes-discovery-client.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.micronaut.kubernetes%22%20AND%20a:%22micronaut-kubernetes-discovery-client%22)
![Snapshot](https://img.shields.io/badge/dynamic/xml?color=yellow&label=Snapshot&query=%2F%2Fmetadata%2Fversioning%2Flatest&url=https%3A%2F%2Fs01.oss.sonatype.org%2Fcontent%2Frepositories%2Fsnapshots%2Fio%2Fmicronaut%2Fkubernetes%2Fmicronaut-kubernetes-client%2Fmaven-metadata.xml)
[![Build Status](https://travis-ci.org/micronaut-projects/micronaut-kubernetes.svg?branch=master)](https://travis-ci.org/micronaut-projects/micronaut-kubernetes)
[![Revved up by Gradle Enterprise](https://img.shields.io/badge/Revved%20up%20by-Gradle%20Enterprise-06A0CE?logo=Gradle&labelColor=02303A)](https://ge.micronaut.io/scans)

This project includes integration between [Micronaut](http://micronaut.io) and [Kubernetes](https://kubernetes.io).

## Documentation

See the [Documentation](https://micronaut-projects.github.io/micronaut-kubernetes/latest/guide) for more information.

See the [Snapshot Documentation](https://micronaut-projects.github.io/micronaut-kubernetes/snapshot/guide) for the 
current development docs.

## Contributing Code

If you wish to contribute to the development of this project please read the [CONTRIBUTING.md](CONTRIBUTING.md)

## Snapshots and Releases

Snaphots are automatically published to [JFrog OSS](https://s01.oss.sonatype.org/content/repositories/snapshots/io/micronaut/) using [Github Actions](https://github.com/micronaut-projects/micronaut-kubernetes/actions).

See the documentation in the [Micronaut Docs](https://docs.micronaut.io/latest/guide/index.html#usingsnapshots) for how to configure your build to use snapshots.

Releases are published to JCenter and Maven Central via [Github Actions](https://github.com/micronaut-projects/micronaut-kubernetes/actions).

A release is performed with the following steps:

* [Publish the draft release](https://github.com/micronaut-projects/micronaut-kubernetes/releases). There should be already a draft release created, edit and publish it. The Git Tag should start with `v`. For example `v1.0.0`.
* [Monitor the Workflow](https://github.com/micronaut-projects/micronaut-kubernetes/actions?query=workflow%3ARelease) to check it passed successfully.
* Celebrate!

