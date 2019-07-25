package io.micronaut.kubernetes.client.v1.pods;

/**
 * Information about the running container.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
public class ContainerStatus {

    private String name;
    private String image;

    /**
     * @return Container name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name container name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return Container image
     */
    public String getImage() {
        return image;
    }

    /**
     * @param image container image
     */
    public void setImage(String image) {
        this.image = image;
    }

    @Override
    public String toString() {
        return "ContainerStatus{" +
                "name='" + name + '\'' +
                ", image='" + image + '\'' +
                '}';
    }
}
