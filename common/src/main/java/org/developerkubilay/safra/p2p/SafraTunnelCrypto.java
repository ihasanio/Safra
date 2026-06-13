package org.developerkubilay.safra.p2p;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.spec.NamedParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

/**
 * Per-connection end-to-end encryption for the reliable UDP tunnel.
 *
 * <p>Handshake: each side generates an ephemeral X25519 key pair and piggy-backs
 * its public key on the OPEN / OPEN_ACK handshake packets. Once the peer's public
 * key is known, an ECDH shared secret is derived and split (SHA-256 with
 * directional labels) into two independent ChaCha20-Poly1305 keys, one per
 * direction. DATA payloads are sealed with the sender's key and a nonce derived
 * from the packet sequence number, which is unique per direction and never
 * reused, so the nonce never has to travel on the wire.</p>
 *
 * <p>If the remote peer presents no public key (older build, or encryption
 * disabled) the session silently falls back to plaintext, preserving wire
 * compatibility.</p>
 */
final class SafraTunnelCrypto {
    private static final String KEY_ALGORITHM = "XDH";
    private static final String CURVE = "X25519";
    private static final String CIPHER = "ChaCha20-Poly1305";
    private static final String MAC_DIGEST = "SHA-256";
    private static final byte[] LABEL_INITIATOR_TO_RESPONDER = "safra-tunnel-i2r".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] LABEL_RESPONDER_TO_INITIATOR = "safra-tunnel-r2i".getBytes(StandardCharsets.US_ASCII);

    private final KeyPair keyPair;
    private final byte[] localPublicKey;

    private final Object encryptLock = new Object();
    private final Object decryptLock = new Object();
    private final Cipher encryptCipher;
    private final Cipher decryptCipher;

    private volatile SecretKeySpec sendKey;
    private volatile SecretKeySpec receiveKey;
    private volatile boolean established;

    SafraTunnelCrypto() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
        generator.initialize(new NamedParameterSpec(CURVE));
        this.keyPair = generator.generateKeyPair();
        this.localPublicKey = keyPair.getPublic().getEncoded();
        this.encryptCipher = Cipher.getInstance(CIPHER);
        this.decryptCipher = Cipher.getInstance(CIPHER);
    }

    /** The X.509-encoded local public key to advertise in the handshake. */
    byte[] localPublicKey() {
        return localPublicKey;
    }

    boolean isEstablished() {
        return established;
    }

    /**
     * Derives the directional session keys from the peer's advertised public key.
     *
     * @param remotePublicKey the peer handshake payload (X.509-encoded X25519 key)
     * @param initiator       true on the side that sent OPEN, false on the side that answers
     * @return true if encryption was established, false if the peer key was absent or invalid
     */
    boolean establish(byte[] remotePublicKey, boolean initiator) {
        if (remotePublicKey == null || remotePublicKey.length == 0) {
            return false;
        }

        try {
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            PublicKey peerKey = keyFactory.generatePublic(new X509EncodedKeySpec(remotePublicKey));

            KeyAgreement agreement = KeyAgreement.getInstance(KEY_ALGORITHM);
            agreement.init(keyPair.getPrivate());
            agreement.doPhase(peerKey, true);
            byte[] sharedSecret = agreement.generateSecret();

            SecretKeySpec initiatorToResponder = deriveKey(sharedSecret, LABEL_INITIATOR_TO_RESPONDER);
            SecretKeySpec responderToInitiator = deriveKey(sharedSecret, LABEL_RESPONDER_TO_INITIATOR);
            Arrays.fill(sharedSecret, (byte) 0);

            if (initiator) {
                this.sendKey = initiatorToResponder;
                this.receiveKey = responderToInitiator;
            } else {
                this.sendKey = responderToInitiator;
                this.receiveKey = initiatorToResponder;
            }
            this.established = true;
            return true;
        } catch (GeneralSecurityException exception) {
            return false;
        }
    }

    /** Seals a DATA payload; the returned bytes are {@code plaintext.length + AEAD tag}. */
    byte[] encrypt(byte[] plaintext, int sequence) throws GeneralSecurityException {
        synchronized (encryptLock) {
            encryptCipher.init(Cipher.ENCRYPT_MODE, sendKey, nonce(sequence));
            return encryptCipher.doFinal(plaintext);
        }
    }

    /** Opens a sealed DATA payload, verifying the authentication tag. */
    byte[] decrypt(byte[] ciphertext, int sequence) throws GeneralSecurityException {
        synchronized (decryptLock) {
            decryptCipher.init(Cipher.DECRYPT_MODE, receiveKey, nonce(sequence));
            return decryptCipher.doFinal(ciphertext);
        }
    }

    private static SecretKeySpec deriveKey(byte[] sharedSecret, byte[] label) throws GeneralSecurityException {
        MessageDigest digest = MessageDigest.getInstance(MAC_DIGEST);
        digest.update(sharedSecret);
        digest.update(label);
        return new SecretKeySpec(digest.digest(), "ChaCha20");
    }

    private static IvParameterSpec nonce(int sequence) {
        // 12-byte nonce; the sequence (unique per direction, never reused) fills the
        // low 4 bytes, the rest stay zero. Each direction uses a distinct key, so even
        // identical nonces across directions are safe.
        byte[] nonce = new byte[12];
        nonce[8] = (byte) (sequence >>> 24);
        nonce[9] = (byte) (sequence >>> 16);
        nonce[10] = (byte) (sequence >>> 8);
        nonce[11] = (byte) sequence;
        return new IvParameterSpec(nonce);
    }
}
