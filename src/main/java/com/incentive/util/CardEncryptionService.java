package com.incentive.util;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

@ApplicationScoped
public class CardEncryptionService {

    @ConfigProperty(name = "card.encryption.key")
    String hexKey; // 32 hex chars = 16 bytes (AES-128)

    /**
     * Decrypts AES-GCM encrypted data from frontend.
     * Format: base64(12-byte-IV + ciphertext)
     */
    public String decrypt(String encryptedBase64) {
        try {
            byte[] data = Base64.getDecoder().decode(encryptedBase64);
            byte[] iv = Arrays.copyOfRange(data, 0, 12);
            byte[] ciphertext = Arrays.copyOfRange(data, 12, data.length);
            SecretKeySpec keySpec = new SecretKeySpec(hexToBytes(hexKey), "AES");
            GCMParameterSpec paramSpec = new GCMParameterSpec(128, iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, paramSpec);
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao descriptografar dado do cartão", e);
        }
    }

    private byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                 + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
