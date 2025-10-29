/**
 * Configuration constants for the Multicast application
 */
public class MulticastConfig {
    // Multicast group address (must be in range 224.0.0.0 to 239.255.255.255)
    public static final String MULTICAST_ADDRESS = "230.0.0.1";

    // Port number for multicast communication
    public static final int PORT = 4446;

    // Maximum packet size
    public static final int BUFFER_SIZE = 1024;
}

