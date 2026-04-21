package com.fivefy.domain.user.entity;

import com.fivefy.common.entity.BaseEntity;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.user.enums.UserRole;
import com.fivefy.domain.user.enums.UserStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

import static com.fivefy.common.util.ValidationUtils.validateNonNull;
import static com.fivefy.domain.user.enums.UserErrorCode.ERR_USER_ALREADY_DELETED;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    private LocalDateTime lastActiveAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    public static User create(String email, String encodedPassword, String name) {
        validateNonNull(email, "email");
        validateNonNull(encodedPassword, "password");
        validateNonNull(name, "name");

        User user = new User();
        user.email = email;
        user.password = encodedPassword;
        user.name = name;
        user.role = UserRole.USER;
        user.status = UserStatus.ACTIVE;

        return user;
    }

    public void reactivate() {
        this.status = UserStatus.ACTIVE;
        this.lastActiveAt = LocalDateTime.now();
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void delete() {
        if (this.deletedAt != null) {
            throw new BusinessException(ERR_USER_ALREADY_DELETED);
        }
        this.status = UserStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }
}
