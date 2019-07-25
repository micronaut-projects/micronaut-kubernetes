package io.micronaut.kubernetes.client.v1.pods;

import java.util.List;

/**
 * Pod status information.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
public class PodStatus {

    private String phase;
    private String hostIP;
    private String podIP;
    private List<ContainerStatus> containerStatuses;

    /**
     * @return Execution phase (eg: Running)
     */
    public String getPhase() {
        return phase;
    }

    /**
     * @param phase Execution phase (eg: Running)
     */
    public void setPhase(String phase) {
        this.phase = phase;
    }

    /**
     * @return Host IP address
     */
    public String getHostIP() {
        return hostIP;
    }

    /**
     * @param hostIP Host IP address
     */
    public void setHostIP(String hostIP) {
        this.hostIP = hostIP;
    }

    /**
     * @return Pod IP address
     */
    public String getPodIP() {
        return podIP;
    }

    /**
     * @param podIP Pod IP address
     */
    public void setPodIP(String podIP) {
        this.podIP = podIP;
    }

    /**
     * @return Status of all the containers running in this Pod
     */
    public List<ContainerStatus> getContainerStatuses() {
        return containerStatuses;
    }

    /**
     * @param containerStatuses Status of all the containers running in this Pod
     */
    public void setContainerStatuses(List<ContainerStatus> containerStatuses) {
        this.containerStatuses = containerStatuses;
    }

    @Override
    public String toString() {
        return "PodStatus{" +
                "phase='" + phase + '\'' +
                ", hostIP='" + hostIP + '\'' +
                ", podIP='" + podIP + '\'' +
                ", containerStatuses=" + containerStatuses +
                '}';
    }
}
