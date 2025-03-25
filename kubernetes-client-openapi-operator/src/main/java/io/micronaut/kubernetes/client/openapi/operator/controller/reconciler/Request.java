package io.micronaut.kubernetes.client.openapi.operator.controller.reconciler;

/**
 * The type Request contains the information necessary to reconcile a resource object. This includes
 * the information to uniquely identify the object - its Name and Namespace. Note that it does NOT
 * contain information about any specific Event or the object contents itself.
 *
 * <p>
 * The code has been copied from the official client and modified:
 * <a href="https://github.com/kubernetes-client/java/blob/v21.0.2/extended/src/main/java/io/kubernetes/client/extended/controller/reconciler/Request.java">Request</a>
 * </p>
 */
public class Request {

    private String name;
    private String namespace;

    /**
     * Instantiates a new Request specifying the name. Works for cluster-scoped resource.
     *
     * @param name the resource object name
     */
    public Request(String name) {
        this(null, name);
    }

    /**
     * Instantiates a new Request specifying the name and namespace. Works for namespace-scoped
     * resource.
     *
     * @param namespace the resource object namespace
     * @param name      the resource object name
     */
    public Request(String namespace, String name) {
        this.name = name;
        this.namespace = namespace;
    }

    /**
     * Gets namespace.
     *
     * @return the namespace
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Sets namespace.
     *
     * @param namespace the namespace
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * Gets name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Request{" + "name='" + name + '\'' + ", namespace='" + namespace + '\'' + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Request request = (Request) o;

        if (name != null ? !name.equals(request.name) : request.name != null) return false;
        return namespace != null ? namespace.equals(request.namespace) : request.namespace == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (namespace != null ? namespace.hashCode() : 0);
        return result;
    }

    /**
     * Sets name.
     *
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }
}
