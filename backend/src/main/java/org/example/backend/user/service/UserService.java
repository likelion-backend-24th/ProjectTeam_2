package org.example.backend.user.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.user.dto.LoginRequest;
import org.example.backend.user.dto.SignupRequest;
import org.example.backend.user.dto.TokenResponse;
import org.example.backend.user.entity.AccountStatus;
import org.example.backend.user.entity.Role;
import org.example.backend.user.entity.User;
import org.example.backend.user.exception.ResourceNotFoundException;
import org.example.backend.user.repository.UserRepository;
import org.example.backend.user.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    //회원가입
    @Transactional
    public void signup(SignupRequest signupRequest){
        if(userRepository.existsByUsername(signupRequest.getUsername())){
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }
        User user = new User();
        user.setName(signupRequest.getName());
        user.setUsername(signupRequest.getUsername());
        user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
        user.setNickname(signupRequest.getNickname());
        user.setRole(Role.USER);
        user.setStatus(AccountStatus.ACTIVE);

        userRepository.save(user);
    }

    //로그인
    public TokenResponse login(LoginRequest loginRequest){
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(()-> new ResourceNotFoundException("이메일 또는 비밀번호가 일치하지 않아 세수하고 다시와서 시도해라"));

        if(!passwordEncoder.matches(loginRequest.getPassword(),user.getPassword())){
            throw new ResourceNotFoundException("이메일 또는 비밀번호가 일치하지 않습니다.");
        }
        String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());

        // 리프레쉬토큰 디비에 저장해야함 추후에 할 예정
        return new TokenResponse(accessToken, refreshToken);
    }

    //재발급


    //로그아웃
    public void logout(String username){
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
        // DB에서 refresh토큰 삭제해야함 아직 안한거임
    }

}