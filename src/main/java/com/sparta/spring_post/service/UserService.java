package com.sparta.spring_post.service;

import com.sparta.spring_post.dto.LoginRequestDto;
import com.sparta.spring_post.dto.SignupRequestDto;
import com.sparta.spring_post.dto.UserResponseDto;
import com.sparta.spring_post.entity.RefreshToken;
import com.sparta.spring_post.entity.RoleType;
import com.sparta.spring_post.entity.Users;
import com.sparta.spring_post.exception.CustomException;
import com.sparta.spring_post.jwt.dto.TokenDto;
import com.sparta.spring_post.jwt.util.JwtUtil;
import com.sparta.spring_post.repository.RefreshTokenRepository;
import com.sparta.spring_post.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

import static com.sparta.spring_post.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
public class UserService {

    // UserRepository 연결
    private final UserRepository userRepository;
    // JwtUtil 연결
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private static final String ADMIN_TOKEN = "AAABnvxRVklrnYxKZ0aHgTBcXukeZygoC";
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public UserResponseDto<Users> signup(SignupRequestDto signupRequestDto) {
        String username = signupRequestDto.getUsername();
        String password = passwordEncoder.encode(signupRequestDto.getPassword());

        // 회원 중복 확인
        Optional<Users> found = userRepository.findByUsername(username);
        if (found.isPresent()) {
            throw new CustomException(INVALID_USER_EXISTENCE);
        }

        // 관리자 확인
        RoleType role = RoleType.USER;
        if (signupRequestDto.isAdmin()) {
            if (!signupRequestDto.getAdminToken().equals(ADMIN_TOKEN)) {
                throw new CustomException(INVALID_ADMIN_PASSWORD);
            }
            role = RoleType.ADMIN;
        }

        Users users = new Users(username, password, role);
        userRepository.save(users);
        return UserResponseDto.setSuccess("회원가입 성공!");
    }

    @Transactional
    public UserResponseDto<Users> login(LoginRequestDto loginRequestDto, HttpServletResponse httpServletResponse) {
        String username = loginRequestDto.getUsername();
        String password = loginRequestDto.getPassword();

        // 사용자 확인
        Users user = userRepository.findByUsername(username).orElseThrow(
                () -> new CustomException(USER_NOT_FOUND)
        );

        // 비밀번호 확인
        if(!passwordEncoder.matches(password, user.getPassword())){
            throw new CustomException(INVALID_USER_PASSWORD);
        }

        TokenDto tokenDto = jwtUtil.createAllToken(username, user.getRole());

        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByUsername(username);

        if (refreshToken.isPresent()) {
            refreshTokenRepository.save(refreshToken.get().updateToken(tokenDto.getRefreshToken()));
        } else {
            RefreshToken newToken = new RefreshToken(tokenDto.getRefreshToken(), username);
            refreshTokenRepository.save(newToken);
        }

        setHeader(httpServletResponse, tokenDto);
        return UserResponseDto.setSuccess("로그인 성공!");
    }

    private void setHeader(HttpServletResponse httpServletResponse, TokenDto tokenDto) {
        httpServletResponse.addHeader(JwtUtil.ACCESS_TOKEN, tokenDto.getAccessToken());
        httpServletResponse.addHeader(JwtUtil.REFRESH_TOKEN, tokenDto.getRefreshToken());
    }

}