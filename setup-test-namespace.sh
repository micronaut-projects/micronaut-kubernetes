#!/usr/bin/env sh

NAMESPACE=${1:=micronaut-kubernetes}
DELETE_NAMESPACE=${2:=false}

echo "Configuration:"
echo "\tNAMESPACE:        ${NAMESPACE}"
echo "\tDELETE_NAMESPACE: ${DELETE_NAMESPACE}"

#
# Delete namespace if exists and it is requested by second parameter
# that is either "true" or "yes"
if [ "${DELETE_NAMESPACE}" = "true" ] || [ "${DELETE_NAMESPACE}" = "yes" ]; then
  kubectl get ns ${NAMESPACE} > /dev/null 2>&1
  if [ $? -eq 0 ]; then
    echo "Deleting namespace ${NAMESPACE}"
    kubectl delete ns ${NAMESPACE} --wait=true
  fi
fi

# Create namespace and switch context to that namespace
kubectl create namespace ${NAMESPACE}
kubectl config set-context --current --namespace=${NAMESPACE}

# Create role and role binding
kubectl create role service-discoverer --verb=get,list,watch --resource=services,endpoints,configmaps,secrets,pods
kubectl create rolebinding default-service-discoverer --role service-discoverer --serviceaccount=${NAMESPACE}:default

# Create secrets and config maps
kubectl create configmap game-config-properties --from-file=kubernetes-discovery-client/src/k8s/game.properties
kubectl create configmap game-config-yml --from-file=kubernetes-discovery-client/src/k8s/game.yml
kubectl create configmap game-config-json --from-file=kubernetes-discovery-client/src/k8s/game.json
kubectl create configmap literal-config --from-literal=special.how=very --from-literal=special.type=charm
kubectl create secret generic test-secret --from-literal=username='my-app' --from-literal=password='39528$vdg7Jb'
kubectl create secret generic another-secret --from-literal=secretProperty='secretValue'
kubectl create secret generic mounted-secret --from-literal=mountedVolumeKey='mountedVolumeValue'

kubectl label configmap game-config-yml app=game
kubectl label configmap literal-config app=game
kubectl label secret another-secret app=game

# Create and expose deployment - example service
cat <<EOF | kubectl apply -f -
apiVersion: apps/v1
kind: Deployment
metadata:
  name: example-service
spec:
  selector:
    matchLabels:
      app: example-service
  replicas: 2
  template:
    metadata:
      labels:
        app: example-service
    spec:
      containers:
        - name: example-service
          image: micronaut-kubernetes-example-service
          imagePullPolicy: "Never"
          volumeMounts:
            - name: secrets
              mountPath: "/etc/example-service/secrets"
              readOnly: true
          env:
            - name: JAVA_TOOL_OPTIONS
              value: "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5004"
          ports:
            - name: http
              containerPort: 8081
            - name: "jvm-debug"
              containerPort: 5004
      volumes:
        - name: secrets
          secret:
            secretName: mounted-secret
            defaultMode: 256
EOF
kubectl expose  deployment example-service --type LoadBalancer --port 8081 --target-port 8081 --labels foo=bar

# Create and expose deployment - example client
cat <<EOF | kubectl apply -f -
apiVersion: apps/v1
kind: Deployment
metadata:
  name: example-client
spec:
  selector:
    matchLabels:
      app: example-client
  template:
    metadata:
      labels:
        app: example-client
    spec:
      containers:
        - name: example-client
          image: micronaut-kubernetes-example-client
          imagePullPolicy: "Never"
          env:
            - name: JAVA_TOOL_OPTIONS
              value: "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
          ports:
            - name: http
              containerPort: 8082
            - name: "jvm-debug"
              containerPort: 5005
EOF
kubectl expose deployment example-client --type LoadBalancer --port 8082 --target-port 8082

# Create and expose deployment - secure deployment
cat <<EOF | kubectl apply -f -
apiVersion: apps/v1
kind: Deployment
metadata:
  name: secure-deployment
spec:
  selector:
    matchLabels:
      app: secure-deployment
  replicas: 1
  template:
    metadata:
      labels:
        app: secure-deployment
    spec:
      containers:
        - name: secure-deployment
          image: nginx
          ports:
            - containerPort: 80
EOF
# with port name
kubectl expose deployment secure-deployment --name secure-service-port-name --type NodePort --port 1234
kubectl patch svc secure-service-port-name --type='json' -p='[{"op": "replace", "path": "/spec/ports/0/name", "value": "https" }]'
# with 443 port
kubectl expose deployment secure-deployment --name secure-service-port-number --type NodePort --port 443
# with secure label
kubectl expose deployment secure-deployment --name secure-service-labels --labels=secure="true" --type NodePort --port 1234
# non secure deployment
kubectl expose deployment secure-deployment --name non-secure-service --type NodePort --port 1234


CLIENT_POD="$(kubectl get pods | grep "example-client" | awk 'FNR <= 1 { print $1 }')"
SERVICE_POD_1="$(kubectl get pods | grep "example-service" | awk 'FNR <= 1 { print $1 }')"
SERVICE_POD_2="$(kubectl get pods | grep "example-service" | awk 'FNR > 1 { print $1 }')"

kubectl wait --for=condition=Ready pod/$SERVICE_POD_1 --timeout=60s
kubectl wait --for=condition=Ready pod/$CLIENT_POD --timeout=60s
kubectl wait --for=condition=Ready pod/$SERVICE_POD_2 --timeout=60s

kubectl describe pods

echo "Client pod logs:"
kubectl logs $CLIENT_POD

echo "Service pod #1 logs:"
kubectl logs $SERVICE_POD_1

echo "Service pod #2 logs:"
kubectl logs $SERVICE_POD_2


# Configure context back to default
kubectl config set-context --current --namespace=default
