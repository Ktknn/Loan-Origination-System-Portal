package com.example.los.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.los.dto.auth.AuthResponse;
import com.example.los.dto.auth.LoginRequest;
import com.example.los.dto.auth.RefreshResponse;
import com.example.los.dto.auth.RegisterRequest;
import com.example.los.entity.RefreshToken;
import com.example.los.entity.User;
import com.example.los.entity.enums.UserStatus;
import com.example.los.repository.UserRepository;
import com.example.los.security.JwtUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtUtils jwtUtils;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    private User mockUser;
    private RefreshToken mockRefreshToken;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .userId("user-001")
                .fullName("Nguyen Van A")
                .email("test@example.com")
                .cccd("012345678901")
                .phoneNumber("0912345678")
                .passwordHash("hashed-password")
                .birthdate(LocalDate.of(1990, 6, 15))
                .status(UserStatus.ACTIVE)
                .build();

        mockRefreshToken = RefreshToken.builder()
                .token("refresh-uuid-token")
                .userId("user-001")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
    }

    // ─── Login ────────────────────────────────────────────────

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("Successful login returns AuthResponse with accessToken")
        void login_successReturnsAuthResponse() {
            LoginRequest req = new LoginRequest();
            req.setEmail("test@example.com");
            req.setPassword("password123");

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
            when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);
            when(jwtUtils.generateAccessToken("user-001", "test@example.com")).thenReturn("access-token-xyz");
            when(refreshTokenService.createRefreshToken("user-001")).thenReturn(mockRefreshToken);

            AuthResponse result = authService.login(req);

            assertThat(result.getAccessToken()).isEqualTo("access-token-xyz");
            assertThat(result.getRefreshToken()).isEqualTo("refresh-uuid-token");
            assertThat(result.getEmail()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("Login with wrong password throws RuntimeException")
        void login_wrongPassword_throws() {
            LoginRequest req = new LoginRequest();
            req.setEmail("test@example.com");
            req.setPassword("wrong-password");

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
            when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Mật khẩu không chính xác");
        }

        @Test
        @DisplayName("Login with non-existent email throws RuntimeException")
        void login_nonExistentEmail_throws() {
            LoginRequest req = new LoginRequest();
            req.setEmail("ghost@example.com");
            req.setPassword("any");

            when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Tài khoản không tồn tại");
        }
    }

    // ─── Register ─────────────────────────────────────────────

    @Nested
    @DisplayName("register()")
    class Register {

        private RegisterRequest buildRegisterRequest() {
            RegisterRequest req = new RegisterRequest();
            req.setFullName("Nguyen Van A");
            req.setEmail("new@example.com");
            req.setCccd("012345678902");
            req.setPassword("password123");
            req.setPhone("0912345679");
            req.setBirthDate("1995-01-15");
            return req;
        }

        @Test
        @DisplayName("Successful register saves user and returns AuthResponse")
        void register_success() {
            RegisterRequest req = buildRegisterRequest();

            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(userRepository.existsByCccd("012345678902")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("hashed-pw");
            when(userRepository.save(any(User.class))).thenReturn(mockUser);
            when(jwtUtils.generateAccessToken(anyString(), anyString())).thenReturn("access-token");
            when(refreshTokenService.createRefreshToken(anyString())).thenReturn(mockRefreshToken);

            AuthResponse result = authService.register(req);

            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo("access-token");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Register with duplicate email throws RuntimeException")
        void register_duplicateEmail_throws() {
            RegisterRequest req = buildRegisterRequest();
            when(userRepository.existsByEmail("new@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Email");
        }

        @Test
        @DisplayName("Register with duplicate CCCD throws RuntimeException")
        void register_duplicateCccd_throws() {
            RegisterRequest req = buildRegisterRequest();
            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(userRepository.existsByCccd("012345678902")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("CCCD");
        }

        @Test
        @DisplayName("Register without birthDate -> birthdate=null, no exception")
        void register_noBirthDate_noException() {
            RegisterRequest req = buildRegisterRequest();
            req.setBirthDate(null);

            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(userRepository.existsByCccd("012345678902")).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(userRepository.save(any())).thenReturn(mockUser);
            when(jwtUtils.generateAccessToken(anyString(), anyString())).thenReturn("token");
            when(refreshTokenService.createRefreshToken(anyString())).thenReturn(mockRefreshToken);

            AuthResponse result = authService.register(req);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Register with invalid birthDate format -> handled gracefully (parse error)")
        void register_invalidBirthDateFormat_throws() {
            RegisterRequest req = buildRegisterRequest();
            req.setBirthDate("32/13/9999"); // invalid format

            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(userRepository.existsByCccd("012345678902")).thenReturn(false);

            // LocalDate.parse with non-ISO format throws DateTimeParseException
            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(Exception.class);
        }
    }

    // ─── Refresh Token ────────────────────────────────────────

    @Nested
    @DisplayName("refresh()")
    class Refresh {

        @Test
        @DisplayName("Valid refresh token returns new accessToken")
        void refresh_valid_returnsNewAccessToken() {
            when(refreshTokenService.validateAndRotate("valid-refresh")).thenReturn(mockRefreshToken);
            when(userRepository.findById("user-001")).thenReturn(Optional.of(mockUser));
            when(jwtUtils.generateAccessToken("user-001", "test@example.com")).thenReturn("new-access-token");

            RefreshResponse result = authService.refresh("valid-refresh");

            assertThat(result.getAccessToken()).isEqualTo("new-access-token");
            assertThat(result.getNewRefreshToken()).isEqualTo("refresh-uuid-token");
        }

        @Test
        @DisplayName("Invalid/expired refresh token throws RuntimeException")
        void refresh_invalid_throws() {
            when(refreshTokenService.validateAndRotate("bad-token"))
                    .thenThrow(new RuntimeException("Refresh token khong hop le."));

            assertThatThrownBy(() -> authService.refresh("bad-token"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("khong hop le");
        }

        @Test
        @DisplayName("Refresh for non-existent userId throws RuntimeException")
        void refresh_userNotFound_throws() {
            when(refreshTokenService.validateAndRotate("valid-refresh")).thenReturn(mockRefreshToken);
            when(userRepository.findById("user-001")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh("valid-refresh"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User");
        }
    }

    // ─── Logout ───────────────────────────────────────────────

    @Nested
    @DisplayName("logout()")
    class Logout {

        @Test
        @DisplayName("Logout calls refreshTokenService.deleteByUserId")
        void logout_callsDeleteByUserId() {
            authService.logout("user-001");
            verify(refreshTokenService).deleteByUserId("user-001");
        }
    }
}