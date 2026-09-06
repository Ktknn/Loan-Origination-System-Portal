package com.example.los.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.los.config.JwtProperties;
import com.example.los.entity.RefreshToken;
import com.example.los.repository.RefreshTokenRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService Unit Tests")
class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtProperties jwtProperties;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        lenient().when(jwtProperties.getRefreshExpirationMs()).thenReturn(604_800_000L); // 7 days
    }

    // ─── createRefreshToken ───────────────────────────────────

    @Nested
    @DisplayName("createRefreshToken()")
    class CreateRefreshToken {

        @Test
        @DisplayName("Deletes existing tokens before creating new one")
        void create_deletesExistingTokensFirst() {
            RefreshToken saved = RefreshToken.builder()
                    .token("new-token-uuid")
                    .userId("user-001")
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build();
            when(refreshTokenRepository.save(any())).thenReturn(saved);

            refreshTokenService.createRefreshToken("user-001");

            verify(refreshTokenRepository).deleteByUserId("user-001");
        }

        @Test
        @DisplayName("Returns saved RefreshToken with correct userId")
        void create_returnsSavedToken() {
            RefreshToken saved = RefreshToken.builder()
                    .token("new-token-uuid")
                    .userId("user-001")
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .createdAt(LocalDateTime.now())
                    .build();
            when(refreshTokenRepository.save(any())).thenReturn(saved);

            RefreshToken result = refreshTokenService.createRefreshToken("user-001");

            assertThat(result.getUserId()).isEqualTo("user-001");
            assertThat(result.getToken()).isNotBlank();
        }

        @Test
        @DisplayName("Token UUID is unique across multiple calls")
        void create_generatesUniqueToken() {
            when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            RefreshToken t1 = refreshTokenService.createRefreshToken("user-001");
            RefreshToken t2 = refreshTokenService.createRefreshToken("user-001");

            assertThat(t1.getToken()).isNotEqualTo(t2.getToken());
        }
    }

    // ─── validateAndRotate ────────────────────────────────────

    @Nested
    @DisplayName("validateAndRotate()")
    class ValidateAndRotate {

        @Test
        @DisplayName("Throws when token not found in DB")
        void rotate_throwsWhenTokenNotFound() {
            when(refreshTokenRepository.findByToken("unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> refreshTokenService.validateAndRotate("unknown"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("khong hop le");
        }

        @Test
        @DisplayName("Throws and deletes when token is expired")
        void rotate_throwsAndDeletesWhenExpired() {
            RefreshToken expired = RefreshToken.builder()
                    .token("expired-token")
                    .userId("user-001")
                    .expiresAt(LocalDateTime.now().minusMinutes(1)) // already expired
                    .build();
            when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expired));

            assertThatThrownBy(() -> refreshTokenService.validateAndRotate("expired-token"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("het han");

            verify(refreshTokenRepository).delete(expired);
        }

        @Test
        @DisplayName("Valid token: deletes old, saves new (rotation)")
        void rotate_deletesOldAndSavesNew() {
            RefreshToken valid = RefreshToken.builder()
                    .token("valid-token")
                    .userId("user-001")
                    .expiresAt(LocalDateTime.now().plusDays(6))
                    .build();
            when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(valid));
            when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            RefreshToken result = refreshTokenService.validateAndRotate("valid-token");

            verify(refreshTokenRepository).delete(valid);
            verify(refreshTokenRepository).save(any(RefreshToken.class));
            assertThat(result.getToken()).isNotEqualTo("valid-token"); // rotated
            assertThat(result.getUserId()).isEqualTo("user-001");
        }

        @Test
        @DisplayName("Rotated token has new expiry in the future")
        void rotate_newTokenExpiryIsInFuture() {
            RefreshToken valid = RefreshToken.builder()
                    .token("valid-token")
                    .userId("user-001")
                    .expiresAt(LocalDateTime.now().plusDays(6))
                    .build();
            when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(valid));
            when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            RefreshToken result = refreshTokenService.validateAndRotate("valid-token");

            assertThat(result.getExpiresAt()).isAfter(LocalDateTime.now());
        }

        @Test
        @DisplayName("Token expiring exactly now -> treated as expired")
        void rotate_tokenExpiringExactlyNow_isExpired() {
            RefreshToken borderline = RefreshToken.builder()
                    .token("border-token")
                    .userId("user-001")
                    .expiresAt(LocalDateTime.now().minusNanos(1)) // just expired
                    .build();
            when(refreshTokenRepository.findByToken("border-token")).thenReturn(Optional.of(borderline));

            assertThatThrownBy(() -> refreshTokenService.validateAndRotate("border-token"))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    // ─── deleteByUserId ───────────────────────────────────────

    @Nested
    @DisplayName("deleteByUserId() - Logout")
    class DeleteByUserId {

        @Test
        @DisplayName("Calls repository deleteByUserId")
        void delete_callsRepositoryDelete() {
            refreshTokenService.deleteByUserId("user-001");
            verify(refreshTokenRepository).deleteByUserId("user-001");
        }

        @Test
        @DisplayName("Does not throw if user has no tokens (idempotent)")
        void delete_idempotent_noThrow() {
            // deleteByUserId on repo does nothing if no tokens -> should not throw
            refreshTokenService.deleteByUserId("user-with-no-tokens");
            verify(refreshTokenRepository).deleteByUserId("user-with-no-tokens");
        }
    }
}