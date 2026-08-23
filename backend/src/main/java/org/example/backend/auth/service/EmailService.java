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

    // 실제 인증코드 이메일 발송
    public void sendVerificationCode(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("prep2gether <" + fromEmail + ">");  //이메일 발송한 우리 이메일 등록한거인데 앞에 우리 웹사이트 이름을 추가해봄.
        message.setTo(toEmail);
        message.setSubject("[prep2gether] 이메일 인증코드입니다.");
        message.setText("prep2gether 회원가입 및 비밀번호 변경 및 찾기 인증코드 \n 인증 화면에 아래의 코드를 입력해 주시기 바랍니다.\n인증코드: " + code + "\n\n인증코드 유효기간 안내:\n● 인증 코드는 1회만 유효합니다.\n● 인증 코드는 메일이 발송된 후 5분이 지나면 만료됩니다.\n● 인증 코드를 재전송하시는 경우, 이전 인증 코드를 사용하실 수 없습니다.\n" +
                "-------------------------------------------------------\n ▽prep2gether 홈페이지\n http://www.prep2gether.duckdns.org");

        javaMailSender.send(message);
    }

    // 전문가 승인 알림 - toEmail은 신청한 사람의 email입니다
    public void sendExpertApproved(String toEmail) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("prep2gether <" + fromEmail + ">");
        message.setTo(toEmail);
        message.setSubject("[prep2gether] 전문가 신청이 승인되었습니다.");
        message.setText("안녕하세요, prep2gether입니다.\n전문가 신청이 승인되었습니다. 이제 전문가 활동을 시작하실 수 있습니다.\n" +
                "-------------------------------------------------------\n ▽prep2gether 홈페이지\n http://www.prep2gether.duckdns.org");

        javaMailSender.send(message);
    }

    // 전문가 반려 알림
    public void sendExpertRejected(String toEmail) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("prep2gether <" + fromEmail + ">");
        message.setTo(toEmail);
        message.setSubject("[prep2gether] 전문가 신청 결과 안내");
        message.setText("안녕하세요, prep2gether입니다.\n아쉽게도 전문가 신청이 반려되었습니다.\n다음 기회에 또 신청해보세요.\n\n 신청해주셔 감사합니다.\n"+
                "-------------------------------------------------------\n ▽prep2gether 홈페이지\n http://www.prep2gether.duckdns.org");

        javaMailSender.send(message);
    }

    // 구독 시작 알림
    public void sendSubscriptionStarted(String toEmail) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("prep2gether <" + fromEmail + ">");
        message.setTo(toEmail);
        message.setSubject("[prep2gether] 프리미엄 구독이 시작되었습니다.");
        message.setText("안녕하세요, prep2gether입니다.\n" +
                "프리미엄 구독(월 9,900원)이 정상적으로 시작되었습니다.\n이제 프리미엄 혜택을 이용하실 수 있습니다.\n\n 프리미엄 구독 혜택:\n" +
                "● 전문가 1:1 상담 무제한\n" +
                "● 스터디 개설·참여 무제한\n" +
                "● 합격자 자소서 자료 열람\n" +
                "● 구독자 전용 스터디 참여\n" +
                "● 기업 인사이트 리포트\n" +
                "-------------------------------------------------------\n ▽prep2gether 홈페이지\n http://www.prep2gether.duckdns.org");

        javaMailSender.send(message);
    }

    // 구독 취소 알림
    public void sendSubscriptionCancelled(String toEmail) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("prep2gether <" + fromEmail + ">");
        message.setTo(toEmail);
        message.setSubject("[prep2gether] 프리미엄 구독이 취소되었습니다.");
        message.setText("안녕하세요, prep2gether입니다.\n프리미엄 구독이 취소되었습니다.\n남은 이용 기간 동안은 계속 혜택을 이용하실 수 있습니다.\n" +
                "-------------------------------------------------------\n ▽prep2gether 홈페이지\n http://www.prep2gether.duckdns.org");

        javaMailSender.send(message);
    }

    // 전문가 피드백 답변 알림 - 피드백 제목으로 일단 매게변수 해놓음.
    public void sendFeedbackAnswered(String toEmail, String title) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("prep2gether <" + fromEmail + ">");
        message.setTo(toEmail);
        message.setSubject("[prep2gether] 문의하신 질문에 답변이 달렸습니다.");
        message.setText("안녕하세요, prep2gether입니다.\n\"" + title + "\" 질문에 전문가 답변이 등록되었습니다.\n지금 확인해보세요!\n" +
                "-------------------------------------------------------\n ▽prep2gether 홈페이지\n http://www.prep2gether.duckdns.org");

        javaMailSender.send(message);
    }

    // 6자리 숫자 코드 생성
    public String generateCode() {
        Random random = new Random();
        int number = random.nextInt(1000000); // 0 ~ 999999
        return String.format("%06d", number);
    }

    // 자동갱신 결제가 처음 실패해서 유예기간(PAST_DUE)에 들어갔을 때 알림.
    // 접근 권한은 이미 즉시 끊겼고, 유예기간 안에 결제수단을 고치면 자동으로 복구됨을 안내.
    public void sendSubscriptionPastDue(String toEmail) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("prep2gether <" + fromEmail + ">");
        message.setTo(toEmail);
        message.setSubject("[prep2gether] 정기결제에 실패했습니다 - 결제수단을 확인해주세요");
        message.setText("안녕하세요, prep2gether입니다.\n등록된 결제수단으로 정기결제를 시도했지만 승인에 실패해 프리미엄 이용이 잠시 중단되었습니다.\n" +
                "3일 안에 결제수단을 업데이트하시면 매일 자동으로 재시도해 원래대로 복구해드려요.\n" +
                "3일이 지나도 결제에 계속 실패하면 구독이 자동으로 종료됩니다.\n" +
                "-------------------------------------------------------\n ▽prep2gether 홈페이지\n http://www.prep2gether.duckdns.org");

        javaMailSender.send(message);
    }

    // 자동갱신 실패로 구독이 종료됐을 때 알림 (사용자가 직접 해지한 게 아니라 카드 문제 등으로 끊긴 경우)
    public void sendSubscriptionRenewalFailed(String toEmail) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("prep2gether <" + fromEmail + ">");
        message.setTo(toEmail);
        message.setSubject("[prep2gether] 정기결제 실패로 프리미엄 구독이 종료되었습니다.");
        message.setText("안녕하세요, prep2gether입니다.\n등록된 결제수단으로 정기결제를 진행했지만 승인에 실패해 프리미엄 구독이 종료되었습니다.\n" +
                "카드 한도, 유효기간 등을 확인하신 후 다시 구독해주시면 프리미엄 혜택을 계속 이용하실 수 있어요.\n" +
                "-------------------------------------------------------\n ▽prep2gether 홈페이지\n http://www.prep2gether.duckdns.org");

        javaMailSender.send(message);
    }
}