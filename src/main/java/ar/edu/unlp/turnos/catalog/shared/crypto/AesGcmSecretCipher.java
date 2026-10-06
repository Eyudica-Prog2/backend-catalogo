package ar.edu.unlp.turnos.catalog.shared.crypto;

import ar.edu.unlp.turnos.catalog.shared.config.SecretsProperties;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * AES-256-GCM cipher with a random 12 byte IV per encryption.
 *
 * <p>The AES key is derived with SHA-256 from {@code APP_SECRETS_KEY}, so the environment
 * only needs to hold a long random phrase. Encoded form is {@code base64(iv || ciphertext)}.</p>
 */
@Component
public class AesGcmSecretCipher implements SecretCipher {

    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private final SecretKeyHolder keyHolder;
    private final SecureRandom random = new SecureRandom();

    public AesGcmSecretCipher(SecretsProperties properties) {
        if (properties == null || properties.key() == null || properties.key().isBlank()) {
            throw new IllegalArgumentException(
                    "APP_SECRETS_KEY must be configured with a non blank value to encrypt persisted secrets.");
        }
        this.keyHolder = new SecretKeyHolder(deriveKey(properties.key()));
    }

    @Override
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keyHolder.key(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] encoded = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, encoded, 0, iv.length);
            System.arraycopy(encrypted, 0, encoded, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(encoded);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Could not encrypt the secret value.", ex);
        }
    }

    @Override
    public String decrypt(String ciphertext) {
        if (ciphertext == null) {
            return null;
        }
        try {
            byte[] encoded = Base64.getDecoder().decode(ciphertext);
            if (encoded.length <= IV_LENGTH_BYTES) {
                throw new IllegalStateException("Could not decrypt the stored secret value.");
            }
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keyHolder.key(),
                    new GCMParameterSpec(TAG_LENGTH_BITS, encoded, 0, IV_LENGTH_BYTES));
            byte[] plain = cipher.doFinal(encoded, IV_LENGTH_BYTES, encoded.length - IV_LENGTH_BYTES);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            throw new IllegalStateException("Could not decrypt the stored secret value.", ex);
        }
    }

    private static SecretKeySpec deriveKey(String source) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(source.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(digest, "AES");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available in this JVM.", ex);
        }
    }

    /**
     * Keeps the derived key in a holder so it is never exposed through a getter.
     */
    private record SecretKeyHolder(SecretKeySpec key) {
    }
}
