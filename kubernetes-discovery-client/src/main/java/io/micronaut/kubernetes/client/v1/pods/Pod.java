package io.micronaut.kubernetes.client.v1.pods;

import io.micronaut.kubernetes.client.v1.Metadata;

/**
 * Represents a Kubernetes Pod.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
public class Pod {

    private Metadata metadata;
    private PodStatus status;

    /**
     * @return metadata
     */
    public Metadata getMetadata() {
        return metadata;
    }

    /**
     * @param metadata metadata
     */
    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    /**
     * @return pod status information
     */
    public PodStatus getStatus() {
        return status;
    }

    /**
     * @param podStatus pod status information
     */
    public void setStatus(PodStatus podStatus) {
        this.status = podStatus;
    }

    @Override
    public String toString() {
        return "Pod{" +
                "metadata=" + metadata +
                ", status=" + status +
                '}';
    }
}
