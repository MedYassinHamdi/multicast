package multicast;

/**
 * Configuration constants for the Multicast application
 */
public class MulticastConfig {
    // Multicast group address (must be in range 224.0.0.0 to 239.255.255.255)
    public static final String MULTICAST_ADDRESS = "230.0.0.1";

    // Port number for multicast communication
    public static final int PORT = 4446;

    // Maximum packet size (increased for image support)
    public static final int BUFFER_SIZE = 65000; // Max UDP packet size

    // Maximum image size (in bytes) - 5 MB
    public static final int MAX_IMAGE_SIZE = 5 * 1024 * 1024;
}

