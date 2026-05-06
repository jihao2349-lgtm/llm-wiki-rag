package com.jihao.aiwiki.domain.vault;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Secret cipher tests.
 *
 * @author jihao
 * @date 2026/05/06
 */
class SecretCipherTest {

    /** Cipher under test */
    private final SecretCipher secretCipher = new SecretCipher();

    /**
     * 验证 API Key 加密后不包含明文且可解密。
     */
    @Test
    void encryptShouldNotExposePlainTextAndDecryptBack() {
        String plainText = "sk-test-secret";

        String cipherText = secretCipher.encrypt(plainText);

        assertThat(cipherText).doesNotContain(plainText);
        assertThat(secretCipher.decrypt(cipherText)).isEqualTo(plainText);
    }

    /**
     * 验证 masked key 可被识别。
     */
    @Test
    void isMaskedShouldDetectMaskedKey() {
        assertThat(secretCipher.isMasked("sk-****cret")).isTrue();
        assertThat(secretCipher.isMasked("sk-test-secret")).isFalse();
    }
}
