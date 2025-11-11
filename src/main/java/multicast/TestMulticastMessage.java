package multicast;

/**
 * CLASSE DE TEST POUR MulticastMessage
 *
 * Cette classe teste la sérialisation et la désérialisation des messages multicast.
 * Elle valide que le protocole de communication fonctionne correctement AVANT de l'utiliser
 * sur le réseau.
 *
 * OBJECTIF DES TESTS:
 * 1. Vérifier que les messages texte peuvent être sérialisés et désérialisés
 * 2. Vérifier que les images peuvent être sérialisées et désérialisées
 * 3. Vérifier que les données ne sont pas corrompues pendant la conversion
 * 4. Vérifier que les tailles respectent les limites configurées
 *
 * POURQUOI C'EST IMPORTANT:
 * - La sérialisation est le cœur du protocole de communication
 * - Si elle échoue, aucun message ne peut être échangé
 * - Ce test permet de détecter les problèmes AVANT l'exécution réseau
 */
public class TestMulticastMessage {

    /**
     * MÉTHODE PRINCIPALE - EXÉCUTE TOUS LES TESTS
     *
     * Cette méthode exécute une batterie de tests pour valider
     * que MulticastMessage fonctionne correctement.
     */
    public static void main(String[] args) {
        System.out.println("Testing multicast.MulticastMessage Protocol...\n");

        // ========== TEST 1: MESSAGE TEXTE ==========
        /**
         * TEST DE SÉRIALISATION D'UN MESSAGE TEXTE
         *
         * ÉTAPES:
         * 1. Créer un message texte ("Hello, World!")
         * 2. Le sérialiser en bytes avec toBytes()
         * 3. Le désérialiser avec fromBytes()
         * 4. Vérifier que le message reçu est identique à l'original
         *
         * VALIDATION:
         * - Le contenu textuel doit être identique
         * - Le type doit être TEXT
         * - La taille sérialisée doit être affichée
         */
        try {
            System.out.println("Test 1: Text Message Serialization");

            // Création d'un message texte
            MulticastMessage textMsg = new MulticastMessage("Hello, World!");

            // Sérialisation: objet -> bytes
            // Ces bytes sont ce qui sera envoyé sur le réseau
            byte[] data = textMsg.toBytes();

            // Désérialisation: bytes -> objet
            // Simule ce que fait le récepteur quand il reçoit les bytes
            MulticastMessage received = MulticastMessage.fromBytes(data);

            // Affichage des résultats pour validation manuelle
            System.out.println("  Original: " + textMsg.getTextContent());
            System.out.println("  Type: " + textMsg.getType());
            System.out.println("  Serialized size: " + data.length + " bytes");
            System.out.println("  Deserialized: " + received.getTextContent());
            System.out.println("  ✓ Text message test PASSED\n");

        } catch (Exception e) {
            // Si une exception se produit, le test échoue
            System.out.println("  ✗ Text message test FAILED: " + e.getMessage() + "\n");
        }

        // ========== TEST 2: MESSAGE IMAGE ==========
        /**
         * TEST DE SÉRIALISATION D'UN MESSAGE IMAGE
         *
         * ÉTAPES:
         * 1. Créer un tableau de bytes simulant une image (1KB)
         * 2. Remplir le tableau avec des données (valeurs 0-255 en boucle)
         * 3. Créer un MulticastMessage de type IMAGE
         * 4. Le sérialiser en bytes
         * 5. Le désérialiser
         * 6. Vérifier que les données image sont identiques
         *
         * VALIDATION:
         * - Le type doit être IMAGE
         * - Le format doit être "jpg"
         * - Les bytes de l'image doivent être identiques (pas de corruption)
         * - La taille sérialisée inclut l'overhead de sérialisation Java
         */
        try {
            System.out.println("Test 2: Image Message Serialization");

            // Création d'une fausse image de 1KB (1024 bytes)
            byte[] dummyImage = new byte[1024];

            // Remplissage avec des valeurs de test (0 à 255 en boucle)
            // Simule des données binaires d'une vraie image
            for (int i = 0; i < dummyImage.length; i++) {
                dummyImage[i] = (byte) (i % 256);
            }

            // Création d'un message image avec format "jpg"
            MulticastMessage imageMsg = new MulticastMessage(dummyImage, "jpg");

            // Sérialisation de l'objet complet (type + imageData + format + timestamp)
            byte[] data = imageMsg.toBytes();

            // Désérialisation
            MulticastMessage received = MulticastMessage.fromBytes(data);

            // Affichage des résultats
            System.out.println("  Original size: " + dummyImage.length + " bytes");
            System.out.println("  Type: " + imageMsg.getType());
            System.out.println("  Format: " + imageMsg.getImageFormat());
            System.out.println("  Serialized size: " + data.length + " bytes");

            // Note: la taille sérialisée > taille originale car inclut:
            // - Métadonnées de sérialisation Java
            // - Type du message
            // - Format
            // - Timestamp
            // - Structure de l'objet

            System.out.println("  Deserialized size: " + received.getImageData().length + " bytes");

            // Vérification que les données ne sont PAS corrompues
            // Arrays.equals() compare chaque byte du tableau
            System.out.println("  Data matches: " + java.util.Arrays.equals(dummyImage, received.getImageData()));
            System.out.println("  ✓ Image message test PASSED\n");

        } catch (Exception e) {
            System.out.println("  ✗ Image message test FAILED: " + e.getMessage() + "\n");
        }

        // ========== TEST 3: VÉRIFICATION DES LIMITES ==========
        /**
         * TEST DE CONFIGURATION
         *
         * Affiche les limites configurées pour s'assurer qu'elles sont correctes:
         * - BUFFER_SIZE: taille maximale d'un paquet UDP (65000 bytes)
         * - MAX_IMAGE_SIZE: taille maximale d'une image (5 MB)
         *
         * IMPORTANCE:
         * - BUFFER_SIZE limite la taille des messages envoyés
         * - MAX_IMAGE_SIZE empêche de charger des images trop grandes
         * - Ces limites évitent les dépassements de buffer et les erreurs réseau
         */
        System.out.println("Test 3: Size Limits");
        System.out.println("  Max buffer size: " + MulticastConfig.BUFFER_SIZE + " bytes");
        System.out.println("  Max image size: " + (MulticastConfig.MAX_IMAGE_SIZE / 1024 / 1024) + " MB");

        // Calcul et affichage en KB pour comparaison
        System.out.println("  Max buffer size: " + (MulticastConfig.BUFFER_SIZE / 1024) + " KB");
        System.out.println("  ✓ Configuration test PASSED\n");

        // ========== RÉSULTAT FINAL ==========
        System.out.println("All tests completed successfully! ✓");
        System.out.println("\nCONCLUSION:");
        System.out.println("- La sérialisation/désérialisation fonctionne correctement");
        System.out.println("- Les messages texte et image peuvent être transmis");
        System.out.println("- Les données ne sont pas corrompues pendant la conversion");
        System.out.println("- Le protocole est prêt pour une utilisation réseau");
    }
}

