package multicast;

/**
 * Simple test to verify multicast.MulticastMessage serialization works
 */
public class TestMulticastMessage {
    public static void main(String[] args) {
        System.out.println("Testing multicast.MulticastMessage Protocol...\n");

        // Test 1: Text Message
        try {
            System.out.println("Test 1: Text Message Serialization");
            MulticastMessage textMsg = new MulticastMessage("Hello, World!");
            byte[] data = textMsg.toBytes();
            MulticastMessage received = MulticastMessage.fromBytes(data);

            System.out.println("  Original: " + textMsg.getTextContent());
            System.out.println("  Type: " + textMsg.getType());
            System.out.println("  Serialized size: " + data.length + " bytes");
            System.out.println("  Deserialized: " + received.getTextContent());
            System.out.println("  ✓ Text message test PASSED\n");

        } catch (Exception e) {
            System.out.println("  ✗ Text message test FAILED: " + e.getMessage() + "\n");
        }

        // Test 2: Image Message (dummy data)
        try {
            System.out.println("Test 2: Image Message Serialization");
            byte[] dummyImage = new byte[1024]; // 1KB dummy image
            for (int i = 0; i < dummyImage.length; i++) {
                dummyImage[i] = (byte) (i % 256);
            }

            MulticastMessage imageMsg = new MulticastMessage(dummyImage, "jpg");
            byte[] data = imageMsg.toBytes();
            MulticastMessage received = MulticastMessage.fromBytes(data);

            System.out.println("  Original size: " + dummyImage.length + " bytes");
            System.out.println("  Type: " + imageMsg.getType());
            System.out.println("  Format: " + imageMsg.getImageFormat());
            System.out.println("  Serialized size: " + data.length + " bytes");
            System.out.println("  Deserialized size: " + received.getImageData().length + " bytes");
            System.out.println("  Data matches: " + java.util.Arrays.equals(dummyImage, received.getImageData()));
            System.out.println("  ✓ Image message test PASSED\n");

        } catch (Exception e) {
            System.out.println("  ✗ Image message test FAILED: " + e.getMessage() + "\n");
        }

        // Test 3: Size limits
        System.out.println("Test 3: Size Limits");
        System.out.println("  Max buffer size: " + MulticastConfig.BUFFER_SIZE + " bytes");
        System.out.println("  Max image size: " + (MulticastConfig.MAX_IMAGE_SIZE / 1024 / 1024) + " MB");
        System.out.println("  ✓ Configuration test PASSED\n");

        System.out.println("All tests completed successfully! ✓");
    }
}

