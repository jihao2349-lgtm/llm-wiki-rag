package com.jihao.aiwiki.domain.vault;

import com.jihao.aiwiki.common.BusinessException;
import com.jihao.aiwiki.common.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 本机应用密钥加解密工具。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Component
public class SecretCipher {

    /** Cipher text version prefix */
    private static final String VERSION_PREFIX = "v1:";

    /** AES-GCM nonce length */
    private static final int IV_LENGTH = 12;

    /** AES-GCM authentication tag length */
    private static final int TAG_LENGTH_BITS = 128;

    /** 随机数生成器 */
    private final SecureRandom secureRandom = new SecureRandom();

    /** AES key */
    private final SecretKeySpec secretKeySpec;

    /**
     * 使用本机环境派生应用密钥。
     */
    public SecretCipher() {
        this.secretKeySpec = new SecretKeySpec(deriveKey(), "AES");
    }

    /**
     * 加密明文密钥。
     *
     * @param plainText 明文
     * @return 加密后的密文
     */
    public String encrypt(String plainText) {
        if (!StringUtils.hasText(plainText)) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);
            return VERSION_PREFIX + Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.LLM_CALL_FAILED, "encrypt api key failed");
        }
    }

    /**
     * 解密密文密钥。
     *
     * @param cipherText 密文
     * @return 明文
     */
    public String decrypt(String cipherText) {
        if (!StringUtils.hasText(cipherText)) {
            return null;
        }
        if (!cipherText.startsWith(VERSION_PREFIX)) {
            throw new BusinessException(ErrorCode.LLM_CALL_FAILED, "api key cipher format invalid");
        }
        try {
            byte[] payload = Base64.getDecoder().decode(cipherText.substring(VERSION_PREFIX.length()));
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.LLM_CALL_FAILED, "decrypt api key failed");
        }
    }

    /**
     * 生成展示用 masked key。
     *
     * @param plainText 明文密钥
     * @return masked key
     */
    public String mask(String plainText) {
        if (!StringUtils.hasText(plainText)) {
            return null;
        }
        String trimmed = plainText.trim();
        if (trimmed.length() <= 8) {
            return "****" + trimmed.substring(Math.max(0, trimmed.length() - 2));
        }
        return trimmed.substring(0, Math.min(3, trimmed.length())) + "****" + trimmed.substring(trimmed.length() - 4);
    }

    /**
     * 判断请求中的 key 是否为前端回传的 masked key。
     *
     * @param value 请求值
     * @return true 表示 masked key
     */
    public boolean isMasked(String value) {
        return StringUtils.hasText(value) && value.contains("****");
    }

    /**
     * 从环境变量、系统属性和本机用户信息派生 AES key。
     *
     * @return 256-bit key
     */
    private byte[] deriveKey() {
        String explicit = System.getProperty("aiwiki.secret.key");
        if (!StringUtils.hasText(explicit)) {
            explicit = System.getenv("AIWIKI_SECRET_KEY");
        }
        String seed = StringUtils.hasText(explicit)
                ? explicit
                : System.getProperty("user.name", "aiwiki") + ":" + System.getProperty("user.home", "local");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(seed.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.LLM_CALL_FAILED, "derive secret key failed");
        }
    }
}
