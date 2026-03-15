package com.koala.koalaback.domain.review.service;

import com.koala.koalaback.domain.order.entity.Order;
import com.koala.koalaback.domain.order.entity.OrderItem;
import com.koala.koalaback.domain.order.repository.OrderItemRepository;
import com.koala.koalaback.domain.review.dto.ReviewDto;
import com.koala.koalaback.domain.review.entity.SkuReview;
import com.koala.koalaback.domain.review.repository.SkuReviewMediaRepository;
import com.koala.koalaback.domain.review.repository.SkuReviewRepository;
import com.koala.koalaback.domain.sku.entity.Sku;
import com.koala.koalaback.domain.sku.repository.SkuReviewStatsRepository;
import com.koala.koalaback.domain.user.entity.User;
import com.koala.koalaback.domain.user.service.UserService;
import com.koala.koalaback.global.exception.BusinessException;
import com.koala.koalaback.global.exception.ErrorCode;
import com.koala.koalaback.global.util.CodeGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @InjectMocks
    private ReviewService reviewService;

    @Mock private SkuReviewRepository skuReviewRepository;
    @Mock private SkuReviewMediaRepository skuReviewMediaRepository;
    @Mock private SkuReviewStatsRepository skuReviewStatsRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private UserService userService;
    @Mock private CodeGenerator codeGenerator;

    @Test
    @DisplayName("리뷰 작성 성공")
    void createReview_success() {
        // given
        Long userId = 1L;
        ReviewDto.CreateRequest req = mock(ReviewDto.CreateRequest.class);
        given(req.getOrderItemId()).willReturn(1L);
        given(req.getRating()).willReturn(5);
        given(req.getTitle()).willReturn("좋아요");
        given(req.getContent()).willReturn("정말 좋은 상품입니다.");
        given(req.getMediaList()).willReturn(Collections.emptyList());

        Sku sku = mock(Sku.class);
        given(sku.getId()).willReturn(1L);
        given(sku.getSkuCode()).willReturn("SKU-001");
        given(sku.getName()).willReturn("테스트 상품");

        Order order = mock(Order.class);

        OrderItem orderItem = mock(OrderItem.class);
        given(orderItem.getSku()).willReturn(sku);
        given(orderItem.getOrder()).willReturn(order);

        User user = mock(User.class);
        given(user.getUserCode()).willReturn("USER-001");
        given(user.getName()).willReturn("테스터");

        given(skuReviewRepository.existsByOrderItemId(1L)).willReturn(false);
        given(orderItemRepository.findByIdAndOrderUserId(1L, userId))
                .willReturn(Optional.of(orderItem));
        given(userService.getUserById(userId)).willReturn(user);
        given(codeGenerator.generateReviewCode()).willReturn("REV-TESTCODE");
        given(skuReviewRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        // when
        ReviewDto.ReviewResponse result = reviewService.createReview(userId, req);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getReviewCode()).isEqualTo("REV-TESTCODE");
        then(orderItem).should().markReviewWritten();
    }

    @Test
    @DisplayName("리뷰 작성 실패 — 이미 작성된 리뷰")
    void createReview_fail_already_exists() {
        // given
        Long userId = 1L;
        ReviewDto.CreateRequest req = mock(ReviewDto.CreateRequest.class);
        given(req.getOrderItemId()).willReturn(1L);
        given(skuReviewRepository.existsByOrderItemId(1L)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(userId, req))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.REVIEW_ALREADY_EXISTS));
    }

    @Test
    @DisplayName("리뷰 삭제 실패 — 본인 리뷰 아님")
    void deleteReview_fail_not_owner() {
        // given
        Long userId = 1L;
        String reviewCode = "REV-001";

        User anotherUser = mock(User.class);
        given(anotherUser.getId()).willReturn(99L); // 다른 유저

        SkuReview review = mock(SkuReview.class);
        given(review.getUser()).willReturn(anotherUser);

        given(skuReviewRepository.findByReviewCode(reviewCode))
                .willReturn(Optional.of(review));

        // when & then
        assertThatThrownBy(() -> reviewService.deleteReview(userId, reviewCode))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.FORBIDDEN));
    }
}