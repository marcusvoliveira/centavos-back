package com.incentive.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@ApplicationScoped
public class ProjectHashService {

    @ConfigProperty(name = "project.hash.secret")
    String secretKey;

    private static final String ALGORITHM = "AES";

    /**
     * Criptografa o ID do projeto e retorna um hash URL-safe
     */
    public String encryptProjectId(Long projectId) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);

            byte[] encrypted = cipher.doFinal(projectId.toString().getBytes(StandardCharsets.UTF_8));

            // Base64 URL-safe encoding
            return Base64.getUrlEncoder().withoutPadding().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao criptografar ID do projeto", e);
        }
    }

    /**
     * Descriptografa o hash e retorna o ID do projeto
     */
    public Long decryptProjectHash(String hash) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);

            // Decodifica Base64 URL-safe
            byte[] decoded = Base64.getUrlDecoder().decode(hash);
            byte[] decrypted = cipher.doFinal(decoded);

            return Long.parseLong(new String(decrypted, StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Hash inválido ou corrompido", e);
        }
    }

    /**
     * Valida se um hash é válido (consegue descriptografar)
     */
    public boolean isValidHash(String hash) {
        try {
            decryptProjectHash(hash);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
