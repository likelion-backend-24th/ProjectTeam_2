package org.example.backend.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
@RequiredArgsConstructor
public class EmailService {

    // JavaMailSender 객체를 자바가 자동으로 등록
    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    // 이게 실제 메일 발송
    public void sendVerificationCode(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("prep2gether <" + fromEmail + ">");  //이메일 발송한 우리 이메일 등록한거인데 앞에 우리 웹사이트 이름을 추가해봄.
        message.setTo(toEmail);
        message.setSubject("[prep2gether] 이메일 인증코드입니다.");
        message.setText("prep2gether 회원가입 및 비밀번호 변경 및 찾기 인증코드 \n 인증 화면에 아래의 코드를 입력해 주시기 바랍니다.\n인증코드: " + code + "\n\n인증코드 유효기간 안내:\n● 인증 코드는 1회만 유효합니다.\n● 인증 코드는 메일이 발송된 후 5분이 지나면 만료됩니다.\n● 인증 코드를 재전송하시는 경우, 이전 인증 코드를 사용하실 수 없습니다.\n" +
                "-------------------------------------------------------\n ▽prep2gether 홈페이지\n http://www.prep2gether.duckdns.org");

        javaMailSender.send(message);
    }

    // 6자리 숫자 코드 생성
    public String generateCode() {
        Random random = new Random();
        int number = random.nextInt(1000000); // 0 ~ 999999
        return String.format("%06d", number);
    }
}