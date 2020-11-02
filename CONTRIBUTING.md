# Contributing Code or Documentation

To work on this project, you need a Kubernetes cluster accessible via `kubectl`. It can be a local cluster based on
[Minikube](https://kubernetes.io/docs/setup/learning-environment/minikube/),
[Docker Desktop for Mac](https://hub.docker.com/editions/community/docker-ce-desktop-mac), or even a remote cluster hosted
on AWS / GCP / Azure / etc.

## Setting up the environment

Note: If you're using minikube, run command below which will configure the docker environment to use the minikube docker runtime
than the system one. This is needed in order to successfully setup the environment:
```shell script
eval $(minikube -p minikube docker-env)
```

There is a script that will take care to create the required resources used in the tests (services, config maps, secrets, 
etc):

```shell script
./setup-kubernetes.sh
```

## Running all the tests

The test suite is composed of:

* Few unit tests.
* Some integration tests that spin a local Micronaut instance, grab a bean from the context, invoke methods and make
  assertions. Those use either `@MicronautTest` or `ApplicationContext.run()`.
* Some functional tests that exercise the sample applications deployed in Kubernetes. They are all the tests located
  inside `examples/micronaut-service` and `examples/micronaut-client`.
  
To run all the tests, simply execute:

`./gradlew test`

## Sample applications

The test infrastructure is composed of 2 sample applications that will be deployed to Kubernetes:

* `example-service`: contains some endpoints to check distributed configuration. There are 2 replicas of it (for 
  load-balancing testing purposes). It is accessible locally at the following ports:
  * `9999` and `9998` to access the application for each of the replicas.
  * `5004` to attach a remote debugger.

* `example-client`: it acts as a service discovery client for `example-service`. It is accessible locally at the 
  following ports:
  * `8888` to access the application.
  * `5005` to attach a remote debugger.

If you make changes (adding endpoints and/or tests) to any of them, you need to execute `setup-kubernetes.sh` again to
have the Docker images built and redeployed to Kubernetes.  

## Tearing down the environment 

All the resources will be created on its own Kubernetes namespace (`micronaut-kubernetes`), which you can destroy 
afterwards to keep your cluster clean. Alternatively, you can run:

```shell script
./cleanup-kubernetes.sh
```

## Checkstyle

Be aware that this project uses Checkstyle, and your PR might fail in Travis if you do not pay attention to any potential
Checkstyle warnings. Make sure you run Checkstyle for your branch before submitting any PR:

```shell script
./gradlew checkstyleMain
```