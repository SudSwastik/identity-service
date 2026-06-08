package com.sudswastik.identityservice.service;

import com.sudswastik.identityservice.dto.auth.MessageResponse;
import com.sudswastik.identityservice.dto.mfa.MfaSetupResponse;
import com.sudswastik.identityservice.dto.mfa.MfaStatusResponse;
import com.sudswastik.identityservice.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CodeMismatchException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.EnableSoftwareTokenMfaException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class MfaService {

    private final CognitoIdentityProviderClient cognitoClient;

    @Value("${aws.cognito.user-pool-id}")
    private String userPoolId;

    @Value("${spring.application.name}")
    private String appName;

    public MfaSetupResponse setupMfa(String accessToken, String username) {
        try {
            var response = cognitoClient.associateSoftwareToken(r -> r.accessToken(accessToken));
            String secretCode = response.secretCode();
            return new MfaSetupResponse(secretCode, buildOtpauthUri(secretCode, username));
        } catch (NotAuthorizedException e) {
            throw new AuthException("Invalid or expired access token.");
        } catch (CognitoIdentityProviderException e) {
            throw new AuthException(e.awsErrorDetails().errorMessage());
        }
    }

    public MessageResponse verifyMfaSetup(String accessToken, String userCode, String cognitoSub) {
        try {
            cognitoClient.verifySoftwareToken(r -> r
                    .accessToken(accessToken)
                    .userCode(userCode)
                    .friendlyDeviceName("Authenticator App"));

            // Enable and set TOTP as the preferred MFA method
            cognitoClient.adminSetUserMFAPreference(r -> r
                    .userPoolId(userPoolId)
                    .username(cognitoSub)
                    .softwareTokenMfaSettings(s -> s.enabled(true).preferredMfa(true)));

            return new MessageResponse("MFA enabled successfully.");
        } catch (CodeMismatchException | EnableSoftwareTokenMfaException e) {
            throw new AuthException("Invalid TOTP code. Please scan the QR code again and retry.");
        } catch (NotAuthorizedException e) {
            throw new AuthException("Invalid or expired access token.");
        } catch (CognitoIdentityProviderException e) {
            throw new AuthException(e.awsErrorDetails().errorMessage());
        }
    }

    public MessageResponse disableMfa(String cognitoSub) {
        try {
            cognitoClient.adminSetUserMFAPreference(r -> r
                    .userPoolId(userPoolId)
                    .username(cognitoSub)
                    .softwareTokenMfaSettings(s -> s.enabled(false).preferredMfa(false)));
            return new MessageResponse("MFA disabled successfully.");
        } catch (CognitoIdentityProviderException e) {
            throw new AuthException(e.awsErrorDetails().errorMessage());
        }
    }

    public MfaStatusResponse getMfaStatus(String cognitoSub) {
        try {
            var user = cognitoClient.adminGetUser(r -> r
                    .userPoolId(userPoolId)
                    .username(cognitoSub));
            boolean enabled = user.userMFASettingList() != null
                    && user.userMFASettingList().contains("SOFTWARE_TOKEN_MFA");
            return new MfaStatusResponse(enabled);
        } catch (CognitoIdentityProviderException e) {
            throw new AuthException(e.awsErrorDetails().errorMessage());
        }
    }

    private String buildOtpauthUri(String secretCode, String username) {
        String label = URLEncoder.encode(appName + ":" + username, StandardCharsets.UTF_8)
                .replace("+", "%20");
        String issuer = URLEncoder.encode(appName, StandardCharsets.UTF_8)
                .replace("+", "%20");
        return "otpauth://totp/" + label + "?secret=" + secretCode + "&issuer=" + issuer;
    }
}
