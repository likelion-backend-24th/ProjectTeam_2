package org.example.backend.payment.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

// 빌링키 암호화/복호화 (저장할 땐 암호화, 조회할 땐 복호화)
@Component
@Converter
public class BillingKeyConverter implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES";

    //암호화/복호화 쓸 비밀키
    @Value("${billing-key.encrypt-secret}")
    private String secret;

    // 엔티티 값 → DB 저장 값 (암호화)
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec());
            byte[] encrypted = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new IllegalStateException("빌링키 암호화 실패", e);
        }
    }

    // DB 저장 값 → 엔티티 값 (복호화)
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec());
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(dbData));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("빌링키 복호화 실패", e);
        }
    }

    private SecretKeySpec keySpec() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(keyBytes, 0, 16, ALGORITHM); // AES-128, 16바이트 키 사용
    }
}