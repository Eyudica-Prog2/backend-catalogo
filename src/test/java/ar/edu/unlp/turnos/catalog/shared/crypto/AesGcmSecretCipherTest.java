package ar.edu.unlp.turnos.catalog.shared.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.unlp.turnos.catalog.shared.config.SecretsProperties;
import org.junit.jupiter.api.Test;

class AesGcmSecretCipherTest {

    private static final String KEY = "a-long-enough-secret-key-for-tests";

    private final AesGcmSecretCipher cipher = new AesGcmSecretCipher(new SecretsProperties(KEY));

    @Test
    void roundTripsTheValue() {
        String encrypted = cipher.encrypt("redis-password");

        assertThat(encrypted).isNotEqualTo("redis-password");
        assertThat(cipher.decrypt(encrypted)).isEqualTo("redis-password");
    }

    @Test
    void usesAFreshIvOnEveryEncryption() {
        String first = cipher.encrypt("same-value");
        String second = cipher.encrypt("same-value");

        assertThat(first).isNotEqualTo(second);
        assertThat(cipher.decrypt(first)).isEqualTo(cipher.decrypt(second));
    }

    @Test
    void keepsNullAsNull() {
        assertThat(cipher.encrypt(null)).isNull();
        assertThat(cipher.decrypt(null)).isNull();
    }

    @Test
    void rejectsCiphertextEncryptedWithAnotherKey() {
        AesGcmSecretCipher other = new AesGcmSecretCipher(new SecretsProperties("another-secret-key"));
        String encrypted = cipher.encrypt("redis-password");

        assertThatThrownBy(() -> other.decrypt(encrypted))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsGarbage() {
        assertThatThrownBy(() -> cipher.decrypt("not-a-ciphertext"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void refusesToStartWithoutAKey() {
        assertThatThrownBy(() -> new AesGcmSecretCipher(new SecretsProperties("  ")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
