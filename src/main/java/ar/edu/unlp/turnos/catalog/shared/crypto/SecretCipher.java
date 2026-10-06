package ar.edu.unlp.turnos.catalog.shared.crypto;

/**
 * Encrypts/decrypts secrets before they are persisted.
 *
 * <p>Implementations must be deterministic only in the sense that the same plaintext
 * decrypts back to itself; ciphertexts are expected to differ on every encryption.</p>
 */
public interface SecretCipher {

    /**
     * @param plaintext secret in clear text, may be null
     * @return non reversible representation, or null when the input is null
     */
    String encrypt(String plaintext);

    /**
     * @param ciphertext value previously produced by {@link #encrypt(String)}, may be null
     * @return original secret, or null when the input is null
     */
    String decrypt(String ciphertext);
}
