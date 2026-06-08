package com.sudswastik.identityservice.service;

import com.sudswastik.identityservice.dto.role.RoleResponse;
import com.sudswastik.identityservice.dto.user.UpdateProfileRequest;
import com.sudswastik.identityservice.dto.user.UserProfileResponse;
import com.sudswastik.identityservice.exception.AuthException;
import com.sudswastik.identityservice.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDisableUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final CognitoIdentityProviderClient cognitoClient;
    private final UserRoleService userRoleService;

    @Value("${aws.cognito.user-pool-id}")
    private String userPoolId;

    public UserProfileResponse getProfile(String cognitoSub) {
        try {
            var user = cognitoClient.adminGetUser(r -> r
                    .userPoolId(userPoolId)
                    .username(cognitoSub));

            String email = attributeValue(user.userAttributes(), "email");
            String name = attributeValue(user.userAttributes(), "name");
            boolean emailVerified = "true".equalsIgnoreCase(
                    attributeValue(user.userAttributes(), "email_verified"));

            Set<RoleResponse> roles = userRoleService.getRolesForUser(cognitoSub).stream()
                    .map(RoleResponse::from)
                    .collect(Collectors.toUnmodifiableSet());

            return new UserProfileResponse(cognitoSub, email, name, emailVerified,
                    user.enabled(), roles);
        } catch (UserNotFoundException e) {
            throw new ResourceNotFoundException("User not found: " + cognitoSub);
        } catch (CognitoIdentityProviderException e) {
            throw new AuthException(e.awsErrorDetails().errorMessage());
        }
    }

    public UserProfileResponse updateProfile(String cognitoSub, UpdateProfileRequest request) {
        try {
            cognitoClient.adminUpdateUserAttributes(r -> r
                    .userPoolId(userPoolId)
                    .username(cognitoSub)
                    .userAttributes(AttributeType.builder()
                            .name("name")
                            .value(request.name())
                            .build()));
            return getProfile(cognitoSub);
        } catch (UserNotFoundException e) {
            throw new ResourceNotFoundException("User not found: " + cognitoSub);
        } catch (CognitoIdentityProviderException e) {
            throw new AuthException(e.awsErrorDetails().errorMessage());
        }
    }

    public void disableUser(String cognitoSub) {
        try {
            cognitoClient.adminDisableUser(AdminDisableUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(cognitoSub)
                    .build());
        } catch (UserNotFoundException e) {
            throw new ResourceNotFoundException("User not found: " + cognitoSub);
        } catch (CognitoIdentityProviderException e) {
            throw new AuthException(e.awsErrorDetails().errorMessage());
        }
    }

    public void enableUser(String cognitoSub) {
        try {
            cognitoClient.adminEnableUser(r -> r
                    .userPoolId(userPoolId)
                    .username(cognitoSub));
        } catch (UserNotFoundException e) {
            throw new ResourceNotFoundException("User not found: " + cognitoSub);
        } catch (CognitoIdentityProviderException e) {
            throw new AuthException(e.awsErrorDetails().errorMessage());
        }
    }

    private String attributeValue(List<AttributeType> attributes, String name) {
        return attributes.stream()
                .filter(a -> a.name().equals(name))
                .map(AttributeType::value)
                .findFirst()
                .orElse(null);
    }
}
