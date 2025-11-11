package multicast;

/**
 * CLASSE DE CONFIGURATION MULTICAST
 *
 * Cette classe contient toutes les constantes de configuration pour l'application multicast.
 * Les constantes sont définies comme 'static final' pour être accessibles partout sans instanciation.
 *
 * PRINCIPE DU MULTICAST:
 * - Le multicast IP permet d'envoyer des données à plusieurs destinataires simultanément
 * - Utilise des adresses IP spéciales de classe D (224.0.0.0 à 239.255.255.255)
 * - Plus efficace que l'unicast (envoi individuel) ou le broadcast (envoi à tous)
 * - Fonctionne sur UDP (protocole non connecté, sans garantie de livraison)
 */
public class MulticastConfig {

    /**
     * ADRESSE DU GROUPE MULTICAST
     *
     * - 230.0.0.1 est une adresse multicast valide (plage 224.0.0.0 - 239.255.255.255)
     * - Cette adresse identifie le "groupe" multicast
     * - Tous les émetteurs envoient à cette adresse
     * - Tous les récepteurs doivent "rejoindre" ce groupe pour recevoir les messages
     * - Comparable à une "station de radio" : tout le monde qui "écoute" cette adresse reçoit les données
     *
     * PLAGES D'ADRESSES MULTICAST:
     * - 224.0.0.0 - 224.0.0.255: Adresses locales (ne traversent pas les routeurs)
     * - 224.0.1.0 - 238.255.255.255: Usage général sur Internet
     * - 239.0.0.0 - 239.255.255.255: Adresses privées (équivalent aux adresses IP privées)
     */
    public static final String MULTICAST_ADDRESS = "230.0.0.1";

    /**
     * PORT DE COMMUNICATION
     *
     * - Le port 4446 est utilisé pour écouter et envoyer les paquets multicast
     * - Tous les participants (émetteurs et récepteurs) doivent utiliser le même port
     * - Combiné avec l'adresse multicast, forme un "canal" unique de communication
     * - Les ports > 1024 sont recommandés pour éviter les conflits avec les services système
     */
    public static final int PORT = 4446;

    /**
     * TAILLE MAXIMALE DU BUFFER (PAQUET UDP)
     *
     * - 65000 octets = taille proche du maximum théorique d'un paquet UDP (65535 octets)
     * - En pratique, le MTU (Maximum Transmission Unit) limite souvent à ~1500 octets
     * - Les paquets > MTU sont fragmentés au niveau IP (peut causer des pertes)
     * - Utilisé pour allouer le buffer de réception dans MulticastReceiver
     * - Également utilisé comme limite de taille pour l'envoi dans MulticastSender
     *
     * IMPORTANT: UDP ne garantit pas:
     * - La livraison des paquets (peuvent être perdus)
     * - L'ordre des paquets (peuvent arriver dans le désordre)
     * - L'absence de duplication
     */
    public static final int BUFFER_SIZE = 65000;

    /**
     * TAILLE MAXIMALE D'IMAGE
     *
     * - 5 MB = 5 * 1024 * 1024 octets = 5 242 880 octets
     * - Limite la taille des fichiers images avant l'envoi
     * - Empêche l'envoi d'images trop grandes qui dépasseraient BUFFER_SIZE
     * - Note: une image de 5MB fichier peut générer un paquet sérialisé > 5MB
     *   à cause des métadonnées de sérialisation Java
     */
    public static final int MAX_IMAGE_SIZE = 5 * 1024 * 1024;
}

