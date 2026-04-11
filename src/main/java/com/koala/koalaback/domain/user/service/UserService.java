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
                .phone(formatPhoneToE164(req.getPhone()))
                .build();

        userRepository.save(user);
        return issueTokens(user);
    }

    // ── 로그인 ────────────────────────────────────────────

    @Transactional
    public UserDto.TokenResponse login(UserDto.LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (user.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if ("SUSPENDED".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.USER_SUSPENDED);
        }

        if ("INACTIVE".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.USER_INACTIVE);
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
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
        String formattedPhone = req.getPhone() != null ? formatPhoneToE164(req.getPhone()) : user.getPhone();
        user.updateProfile(
                req.getName() != null ? req.getName() : user.getName(),
                formattedPhone
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

    /**
     * 전화번호를 E.164 국제 표준 형식으로 변환
     * 예: "010-7748-8672" → "+821077488672"
     * 예: "+82107748672" → "+821077488672" (그대로 유지)
     */
    private String formatPhoneToE164(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return null;
        }

        // 숫자만 추출
        String cleaned = phone.replaceAll("[^0-9]", "");

        // 빈 문자열이면 null 반환
        if (cleaned.isEmpty()) {
            return null;
        }

        // 0으로 시작하는 한국 로컬 번호면 +82로 변환 (첫 0 제거)
        if (cleaned.startsWith("0")) {
            return "+82" + cleaned.substring(1);
        }

        // 이미 +로 시작하거나 다른 국가 코드면 그대로 반환
        if (cleaned.length() < 10) {
            return null; // 너무 짧은 번호는 null
        }

        return "+" + cleaned;
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