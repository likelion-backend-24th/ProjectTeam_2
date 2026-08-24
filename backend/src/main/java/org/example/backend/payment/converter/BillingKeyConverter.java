package org.example.backend.payment.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.example.backend.payment.config.PortOneProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * DB에 저장되는 빌링키를 AES/GCM으로 암/복호화한다.
 * billingKeySecret은 길이가 32바이트로 고정되어 있지 않으므로 SHA-256으로 해시해 키 길이를 맞춘다.
 */
@Component
@Converter
public class BillingKeyConverter implements AttributeConverter<String, String> {

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final SecretKeySpec key;

    public BillingKeyConverter(PortOneProperties portOneProperties) {
        this.key = deriveKey(portOneProperties.getBillingKeySecret());
    }

    @Override
    public String convertToDatabaseColumn(String plainBillingKey) {
        if (plainBillingKey == null) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainBillingKey.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("빌링키 암호화 실패", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String storedValue) {
        if (storedValue == null) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(storedValue);
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            byte[] cipherText = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("빌링키 복호화 실패", e);
        }
    }

    private static SecretKeySpec deriveKey(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "portone.billing-key-secret 이 설정되지 않았습니다. 빌링키를 암호화할 수 없어 기동할 수 없습니다.");
        }
        try {
            byte[] hashed = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(hashed, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("빌링키 암호화 키 생성 실패", e);
        }
    }
}
