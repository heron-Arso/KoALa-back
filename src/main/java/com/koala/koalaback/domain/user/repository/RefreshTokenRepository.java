package com.koala.koalaback.domain.user.repository;

import com.koala.koalaback.domain.user.entity.RefreshToken;
import org.springframework.data.repository.CrudRepository;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, Long> {
}