package com.koala.koalaback.domain.user.service;

import com.koala.koalaback.domain.user.dto.UserDto;
import com.koala.koalaback.domain.user.entity.RefreshToken;
import com.koala.koalaback.domain.user.entity.User;
import com.koala.koalaback.domain.user.entity.UserAddress;
import com.koala.koalaback.domain.user.repository.RefreshTokenRepository;
import com.koala.koalaback.domain.user.repository.UserRepository;
import com.koala.koalaback.global.exception.BusinessException;
import com.koala.koalaback.global.exception.ErrorCode;
import com.koala.koalaback.global.security.JwtProvider;
import com.koala.koalaback.global.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final CodeGenerator codeGenerator;

    // ── 회원가입 ──────────────────────────────────────────

    @Transactional
    public UserDto.TokenResponse signup(UserDto.SignupRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        User user = User.builder()
                .userCode(codeGenerator.generateCode())
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .name(req.getName())
                .phone(req.getPhone())
                .build();

        userRepository.save(user);
        return issueTokens(user);
    }

    // ── 로그인 ────────────────────────────────────────────

    @Transactional
    public UserDto.TokenResponse login(UserDto.LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);  // ← 괄호 하나 제거
        }

        if (user.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        user.updateLastLoginAt();
        return issueTokens(user);
    }

    // ── 로그아웃 ──────────────────────────────────────────

    @Transactional
    public void logout(Long userId) {
        refreshTokenRepository.deleteByUserId(String.valueOf(userId));
    }

    // ── 토큰 재발급 ───────────────────────────────────────

    @Transactional
    public UserDto.TokenResponse refresh(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        Long userId = jwtProvider.getUserId(refreshToken);

        RefreshToken savedToken = refreshTokenRepository.findById(String.valueOf(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));

        if (!savedToken.getRefreshToken().equals(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        User user = getUserById(userId);
        return issueTokens(user);
    }

    // ── 프로필 조회 ───────────────────────────────────────

    public UserDto.ProfileResponse getProfile(Long userId) {
        return UserDto.ProfileResponse.from(getUserById(userId));
    }

    // ── 프로필 수정 ───────────────────────────────────────

    @Transactional
    public UserDto.ProfileResponse updateProfile(Long userId,
                                                 UserDto.UpdateProfileRequest req) {
        User user = getUserById(userId);
        user.updateProfile(
                req.getName() != null ? req.getName() : user.getName(),
                req.getPhone() != null ? req.getPhone() : user.getPhone()
        );
        return UserDto.ProfileResponse.from(user);
    }

    // ── 비밀번호 변경 ─────────────────────────────────────

    @Transactional
    public void changePassword(Long userId, UserDto.ChangePasswordRequest req) {
        User user = getUserById(userId);

        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        user.updatePassword(passwordEncoder.encode(req.getNewPassword()));
    }

    // ── 회원 탈퇴 ─────────────────────────────────────────

    @Transactional
    public void withdraw(Long userId) {
        User user = getUserById(userId);
        user.softDelete();
        refreshTokenRepository.deleteByUserId(String.valueOf(userId));
    }

    // ── 주소 조회 ─────────────────────────────────────────

    public List<UserDto.AddressResponse> getAddresses(Long userId) {
        User user = getUserById(userId);
        return user.getAddresses().stream()
                .map(UserDto.AddressResponse::from)
                .toList();
    }

    // ── 주소 추가 ─────────────────────────────────────────

    @Transactional
    public UserDto.AddressResponse createAddress(Long userId,
                                                 UserDto.AddressCreateRequest req) {
        User user = getUserById(userId);

        boolean isDefault = user.getAddresses().isEmpty() ||
                Boolean.TRUE.equals(req.getIsDefault());

        if (isDefault) {
            user.getAddresses().forEach(a -> a.setDefault(false));
        }

        UserAddress address = UserAddress.builder()
                .user(user)
                .label(req.getLabel())
                .recipientName(req.getRecipientName())
                .recipientPhone(req.getRecipientPhone())
                .zipCode(req.getZipCode())
                .address1(req.getAddress1())
                .address2(req.getAddress2())
                .isDefault(isDefault)
                .build();

        user.getAddresses().add(address);
        return UserDto.AddressResponse.from(address);
    }

    // ── 주소 수정 ─────────────────────────────────────────

    @Transactional
    public UserDto.AddressResponse updateAddress(Long userId, Long addressId,
                                                 UserDto.AddressUpdateRequest req) {
        UserAddress address = getUserById(userId).getAddresses().stream()
                .filter(a -> a.getId().equals(addressId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        address.update(
                req.getLabel(),
                req.getRecipientName(),
                req.getRecipientPhone(),
                req.getZipCode(),
                req.getAddress1(),
                req.getAddress2()
        );

        return UserDto.AddressResponse.from(address);
    }

    // ── 기본 배송지 설정 ──────────────────────────────────

    @Transactional
    public void setDefaultAddress(Long userId, Long addressId) {
        User user = getUserById(userId);
        user.getAddresses().forEach(a -> a.setDefault(false));
        user.getAddresses().stream()
                .filter(a -> a.getId().equals(addressId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND))
                .setDefault(true);
    }

    // ── 주소 삭제 ─────────────────────────────────────────

    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        User user = getUserById(userId);
        user.getAddresses().removeIf(a -> a.getId().equals(addressId));
    }

    // ── 공통 유틸 ─────────────────────────────────────────

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    // ── 토큰 발급 공통 로직 ───────────────────────────────

    private UserDto.TokenResponse issueTokens(User user) {
        String accessToken = jwtProvider.createAccessToken(user.getId(), "USER");
        String refreshToken = jwtProvider.createRefreshToken(user.getId());

        refreshTokenRepository.save(
                RefreshToken.builder()
                        .userId(String.valueOf(user.getId()))
                        .refreshToken(refreshToken)
                        .expiry(604800L)
                        .build()
        );

        return UserDto.TokenResponse.of(accessToken, refreshToken);
    }
}