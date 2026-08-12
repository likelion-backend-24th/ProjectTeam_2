package org.example.backend.auth.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.auth.entity.EmailVerification;
import org.example.backend.auth.exception.EmailVerificationErrorCode;
import org.example.backend.auth.repository.EmailVerificationRepository;
import org.example.backend.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final EmailService emailService;


    //인증코드 발송
    @Transactional
    public void sendCode(String email) {
        String code = emailService.generateCode();

        EmailVerification emailVerification = new EmailVerification();
        emailVerification.setEmail(email);
        emailVerification.setCode(code);
        emailVerification.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        emailVerification.setVerified(false);

        emailVerificationRepository.save(emailVerification);

        emailService.sendVerificationCode(email,code);
    }

    // 인증코드 검증
    @Transactional
    public void verifyCode(String email, String code){
        EmailVerification emailVerification = emailVerificationRepository.findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new BusinessException(EmailVerificationErrorCode.INVALID_VERIFICATION_CODE));

        if(emailVerification.getExpiresAt().isBefore(LocalDateTime.now())){
            throw new BusinessException(EmailVerificationErrorCode.VERIFICATION_CODE_EXPIRED);
        }

        if(!emailVerification.getCode().equals(code)){
            throw new BusinessException(EmailVerificationErrorCode.INVALID_VERIFICATION_CODE);
        }
        emailVerification.setVerified(true);
        emailVerificationRepository.save(emailVerification);
    }

    //이메일 인증 완료되었는지 확인 메서드
    public void checkVerified(String email){
        EmailVerification emailVerification = emailVerificationRepository.findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new BusinessException(EmailVerificationErrorCode.EMAIL_NOT_VERIFIED));

        if(!emailVerification.isVerified()){
            throw new BusinessException(EmailVerificationErrorCode.EMAIL_NOT_VERIFIED);
        }
    }


}
