package com.sudswastik.identityservice.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
        name = "permissions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_permission_resource_action",
                columnNames = {"resource", "action"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Permission extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(nullable = false, length = 100)
    private String resource;

    @Column(nullable = false, length = 50)
    private String action;

    public String toPermissionString() {
        return resource + ":" + action;
    }
}
