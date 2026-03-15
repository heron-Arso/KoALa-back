package com.koala.koalaback.domain.user.service;

import com.koala.koalaback.domain.user.dto.UserDto;
import com.koala.koalaback.domain.user.entity.RefreshToken;
import com.koala.koalaback.domain.user.entity.User;
import com.koala.koalaback.domain.user.entity.UserAddress;
import com.koala.koalaback.domain.user.repository.RefreshTokenRepository;
import com.koala.koalaback.domain.user.repository.UserAddressRepository;
import com.koala.koalaback.domain.user.repository.UserRepository;
import com.koala.koalaback.global.exception.BusinessException;
import com.koala.koalaback.global.exception.ErrorCode;
import com.koala.koalaback.global.security.JwtProvider;
import com.koala.koalaback.global.util.CodeGenerator;
import com.koala.koalaback.global.util.PhoneNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserAddressRepository userAddressRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final CodeGenerator codeGenerator;
    private final PhoneNormalizer phoneNormalizer;

    @Value("${jwt.refresh-token-expiry-ms}")
    private long refreshTokenExpiryMs;

    // ── Auth ──────────────────────────────────────────────

    @Transactional
    public UserDto.TokenResponse signup(UserDto.SignupRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        String phone = req.getPhone() != null ? phoneNormalizer.normalize(req.getPhone()) : null;

        User user = User.builder()
                .userCode(codeGenerator.generateCode())
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .name(req.getName())
                .phone(phone)
                .build();

        userRepository.save(user);
        log.info("New user registered: userId={}, email={}", user.getId(), user.getEmail());

        return issueTokens(user);
    }

    @Transactional
    public UserDto.TokenResponse login(UserDto.LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        validateUserStatus(user);
        user.updateLastLoginAt();

        return issueTokens(user);
    }

    @Transactional
    public UserDto.TokenResponse refresh(String refreshToken) {
        Long userId = jwtProvider.getUserId(refreshToken);

        RefreshToken saved = refreshTokenRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        if (!saved.getToken().equals(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        User user = getUserById(userId);
        return issueTokens(user);
    }

    @Transactional
    public void logout(Long userId) {
        refreshTokenRepository.deleteById(userId);
    }

    // ── Profile ───────────────────────────────────────────

    public UserDto.ProfileResponse getProfile(Long userId) {
        return UserDto.ProfileResponse.from(getUserById(userId));
    }

    @Transactional
    public UserDto.ProfileResponse updateProfile(Long userId, UserDto.UpdateProfileRequest req) {
        User user = getUserById(userId);
        String phone = req.getPhone() != null ? phoneNormalizer.normalize(req.getPhone()) : null;
        user.updateProfile(req.getName(), phone);
        return UserDto.ProfileResponse.from(user);
    }

    @Transactional
    public void changePassword(Long userId, UserDto.ChangePasswordRequest req) {
        User user = getUserById(userId);

        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        user.updatePassword(passwordEncoder.encode(req.getNewPassword()));
    }

    @Transactional
    public void withdraw(Long userId) {
        User user = getUserById(userId);
        user.softDelete();
        refreshTokenRepository.deleteById(userId);
    }

    // ── Address ───────────────────────────────────────────

    public List<UserDto.AddressResponse> getAddresses(Long userId) {
        return userAddressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId)
                .stream()
                .map(UserDto.AddressResponse::from)
                .toList();
    }

    @Transactional
    public UserDto.AddressResponse createAddress(Long userId, UserDto.AddressCreateRequest req) {
        User user = getUserById(userId);
        String phone = phoneNormalizer.normalize(req.getRecipientPhone());

        boolean setDefault = Boolean.TRUE.equals(req.getIsDefault());
        if (setDefault) {
            userAddressRepository.clearDefaultByUserId(userId);
        }

        UserAddress address = UserAddress.builder()
                .user(user)
                .label(req.getLabel())
                .recipientName(req.getRecipientName())
                .recipientPhone(phone)
                .zipCode(req.getZipCode())
                .address1(req.getAddress1())
                .address2(req.getAddress2())
                .isDefault(setDefault)
                .build();

        return UserDto.AddressResponse.from(userAddressRepository.save(address));
    }

    @Transactional
    public UserDto.AddressResponse updateAddress(Long userId, Long addressId,
                                                 UserDto.AddressUpdateRequest req) {
        UserAddress address = userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        String phone = phoneNormalizer.normalize(req.getRecipientPhone());
        address.update(req.getLabel(), req.getRecipientName(), phone,
                req.getZipCode(), req.getAddress1(), req.getAddress2());

        return UserDto.AddressResponse.from(address);
    }

    @Transactional
    public void setDefaultAddress(Long userId, Long addressId) {
        userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        userAddressRepository.clearDefaultByUserId(userId);

        UserAddress address = userAddressRepository.findById(addressId).get();
        address.setDefault(true);
    }

    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        UserAddress address = userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        userAddressRepository.delete(address);
    }

    // ── Private helpers ───────────────────────────────────

    private UserDto.TokenResponse issueTokens(User user) {
        String accessToken = jwtProvider.createAccessToken(user.getId(), "USER");
        String refreshToken = jwtProvider.createRefreshToken(user.getId());

        long expirySeconds = refreshTokenExpiryMs / 1000;
        refreshTokenRepository.save(new RefreshToken(user.getId(), refreshToken, expirySeconds));

        return UserDto.TokenResponse.of(accessToken, refreshToken);
    }

    private void validateUserStatus(User user) {
        if ("SUSPENDED".equals(user.getStatus())) throw new BusinessException(ErrorCode.USER_SUSPENDED);
        if ("INACTIVE".equals(user.getStatus())) throw new BusinessException(ErrorCode.USER_INACTIVE);
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}