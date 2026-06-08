package com.sudswastik.identityservice.service;

import com.sudswastik.identityservice.dto.auth.AuthResponse;
import com.sudswastik.identityservice.dto.auth.ForgotPasswordRequest;
import com.sudswastik.identityservice.dto.auth.LoginRequest;
import com.sudswastik.identityservice.dto.auth.MessageResponse;
import com.sudswastik.identityservice.dto.auth.MfaChallengeRequest;
import com.sudswastik.identityservice.dto.auth.RefreshTokenRequest;
import com.sudswastik.identityservice.dto.auth.RegisterRequest;
import com.sudswastik.identityservice.dto.auth.ResendVerificationRequest;
import com.sudswastik.identityservice.dto.auth.ResetPasswordRequest;
import com.sudswastik.identityservice.dto.auth.VerifyEmailRequest;
import com.sudswastik.identityservice.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ChallengeNameType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CodeMismatchException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ExpiredCodeException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InvalidPasswordException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotConfirmedException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UsernameExistsException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final CognitoIdentityProviderClient cognitoClient;
    private final EmailService emailService;

    @Value("${aws.cognito.user-pool-id}")
    private String userPoolId;

    @Value("${aws.cognito.client-id}")
    private String clientId;

    @Value("${aws.cognito.client-secret:}")
    private String clientSecret;

    public MessageResponse register(RegisterRequest request) {
        String hash = computeSecretHash(request.email());
        try {
            cognitoClient.signUp(r -> {
                r.clientId(clientId)
                 .username(request.email())
                 .password(request.password())
                 .userAttributes(List.of(
                     AttributeType.builder().name("email").value(request.email()).build(),
                     AttributeType.builder().name("name").value(request.name()).build()
                 ));
                if (hash != null) r.secretHash(hash);
            });
            return new MessageResponse("Registration successful. Please check your email to verify your account.");
        } catch (UsernameExistsException e) {
            throw new AuthException("An account with this email already exists.");
        } catch (InvalidPasswordException e) {
            throw new AuthException("Password does not meet the requirements.");
        } catch (CognitoIdentityProviderException e) {
            throw new AuthException(e.awsErrorDetails().errorMessage());
        }
    }

    public AuthResponse login(LoginRequest request) {
        try {
            var response = cognitoClient.adminInitiateAuth(r -> r
                    .authFlow(AuthFlowType.ADMIN_USER_PASSWORD_AUTH)
                    .userPoolId(userPoolId)
                    .clientId(clientId)
                    .authParameters(Map.of(
                            "USERNAME", request.email(),
                            "PASSWORD", request.password()
                    )));

            if (response.challengeName() != null) {
                return AuthResponse.builder()
                        .challengeName(response.challengeName().name())
                        .session(response.session())
                        .build();
            }

            var result = response.authenticationResult();
            return AuthResponse.builder()
                    .accessToken(result.accessToken())
                    .idToken(result.idToken())
                    .refreshToken(result.refreshToken())
                    .expiresIn(result.expiresIn())
                    .build();
        } catch (NotAuthorizedException | UserNotFoundException e) {
            throw new AuthException("Invalid email or password.");
        } catch (UserNotConfirmedException e) {
            throw new AuthException("Email not verified. Please verify your email before logging in.");
        } catch (CognitoIdentityProviderException e) {
            throw new AuthException(e.awsErrorDetails().errorMessage());
        }
    }

    public AuthResponse respondToMfaChallenge(MfaChallengeRequest request) {
        try {
            var response = cognitoClient.adminRespondToAuthChallenge(r -> r
                    .challengeName(ChallengeNameType.SOFTWARE_TOKEN_MFA)
                    .clientId(clientId)
                    .userPoolId(userPoolId)
                    .session(request.session())
                    .challengeResponses(Map.of(
                            "USERNAME", request.email(),
                            "SOFTWARE_TOKEN_MFA_CODE", request.mfaCode()
                    )));

            var result = response.authenticationResult();
            return AuthResponse.builder()
                    .accessToken(result.accessToken())
                    .idToken(result.idToken())
                    .refreshToken(result.refreshToken())
                    .expiresIn(result.expiresIn())
                    .build();
        } catch (NotAuthorizedException | CodeMismatchException e) {
            throw new AuthException("Invalid MFA code.");
        } catch (ExpiredCodeException e) {
            throw new AuthException("MFA session expired. Please log in again.");
        } catch (CognitoIdentityProviderException e) {
            throw new AuthException(e.awsErrorDetails().errorMessage());
        }
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        try {
            // AdminInitiateAuth avoids needing SECRET_HASH for refresh
            var response = cognitoClient.adminInitiateAuth(r -> r
                    .authFlow(AuthFlowType.REFRESH_TOKEN_AUTH)
                    .userPoolId(userPoolId)
                    .clientId(clientId)
                    .authParameters(Map.of("REFRESH_TOKEN", request.refreshToken())));

            var result = response.authenticationResult();
            // Cognito does not issue a new refresh token on token refresh
            return AuthResponse.builder()
                    .accessToken(result.accessToken())
                    .idToken(result.idToken())
                    .expiresIn(result.expiresIn())
                    .build();
        } catch (NotAuthorizedException e) {
            throw new AuthException("Invalid or expired refresh token.");
        } catch (CognitoIdentityProviderException e) {
            throw new AuthException(e.awsErrorDetails().errorMessage());
        }
    }

    public MessageResponse logout(String accessToken) {
        try {
            cognitoClient.globalSignOut(r -> r.accessToken(accessToken));
            return new MessageResponse("Successfully logged out.");
        } catch (NotAuthorizedException e) {
            throw new AuthException("Invalid or expired access token.");
        } catch (CognitoIdentityProviderException e) {
            throw new AuthException(e.awsErrorDetails().errorMessage());
        }
    }

    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        String hash = computeSecretHash(request.email());
        try {
            cognitoClient.forgotPassword(r -> {
                r.clientId(clientId).username(request.email());
                if (hash != null) r.secretHash(hash);
            });
            return new MessageResponse("If an account with that email exists, a reset code has been sent.");
        } catch (UserNotFoundException e) {
            // Intentionally neutral — don't reveal whether the email is registered
            return new MessageResponse("If an account with that email exists, a reset code has been sent.");
        } catch (CognitoIdentityProviderException e) {
            throw new AuthException(e.awsErrorDetails().errorMessage());
        }
    }

    public MessageResponse resetPassword(ResetPasswordRequest request) {
        String hash = computeSecretHash(request.email());
        try {
            cognitoClient.confirmForgotPassword(r -> {
                r.clientId(clientId)
                 .username(request.email())
                 .confirmationCode(request.confirmationCode())
                 .password(request.newPassword());
                if (hash != null) r.secretHash(hash);
            });
            return new MessageResponse("Password reset successfully.");
        } catch (CodeMismatchException | ExpiredCodeException e) {
            throw new AuthException("Invalid or expired confirmation code.");
        } catch (InvalidPasswordException e) {
            throw new AuthException("Password does not meet the requirements.");
        } catch (CognitoIdentityProviderException e) {
            throw new AuthException(e.awsErrorDetails().errorMessage());
        }
    }

    public MessageResponse verifyEmail(VerifyEmailRequest request) {
        String hash = computeSecretHash(request.email());
        try {
            cognitoClient.confirmSignUp(r -> {
                r.clientId(clientId)
                 .username(request.email())
                 .confirmationCode(request.confirmationCode());
                if (hash != null) r.secretHash(hash);
            });
            emailService.sendWelcomeEmail(request.email());
            return new MessageResponse("Email verified successfully. Welcome!");
        } catch (CodeMismatchException | ExpiredCodeException e) {
            throw new AuthException("Invalid or expired confirmation code.");
        } catch (NotAuthorizedException e) {
            throw new AuthException("Account is already confirmed.");
        } catch (CognitoIdentityProviderException e) {
            throw new AuthException(e.awsErrorDetails().errorMessage());
        }
    }

    public MessageResponse resendVerificationCode(ResendVerificationRequest request) {
        String hash = computeSecretHash(request.email());
        try {
            cognitoClient.resendConfirmationCode(r -> {
                r.clientId(clientId).username(request.email());
                if (hash != null) r.secretHash(hash);
            });
            return new MessageResponse("If an unconfirmed account with that email exists, a code has been resent.");
        } catch (UserNotFoundException e) {
            return new MessageResponse("If an unconfirmed account with that email exists, a code has been resent.");
        } catch (CognitoIdentityProviderException e) {
            throw new AuthException(e.awsErrorDetails().errorMessage());
        }
    }

    private String computeSecretHash(String username) {
        if (clientSecret == null || clientSecret.isBlank()) {
            return null;
        }
        try {
            String message = username + clientId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(clientSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to compute Cognito secret hash", e);
        }
    }
}
