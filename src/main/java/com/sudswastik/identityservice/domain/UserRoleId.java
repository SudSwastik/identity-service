package com.sudswastik.identityservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserRoleId implements Serializable {

    @Column(name = "cognito_sub", nullable = false)
    private String cognitoSub;

    @Column(name = "role_id", nullable = false)
    private Long roleId;
}
