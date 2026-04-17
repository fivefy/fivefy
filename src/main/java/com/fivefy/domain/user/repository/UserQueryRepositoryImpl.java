package com.fivefy.domain.user.repository;

import com.fivefy.domain.user.enums.UserStatus;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

import static com.fivefy.domain.user.entity.QUser.user;

@Repository
@RequiredArgsConstructor
public class UserQueryRepositoryImpl implements UserQueryRepository{

    private final JPAQueryFactory queryFactory;

    @Override
    public int anonymizeDeletedUsers(LocalDateTime threshold) {
        return (int) queryFactory
                .update(user)
                .set(user.email,
                        Expressions.stringTemplate(
                                "concat('deleted_', {0}, '@deleted.com')", user.id))
                .set(user.name, "탈퇴한 유저")
                .set(user.password, "DELETED")
                .where(
                        user.deletedAt.lt(threshold),
                        user.status.eq(UserStatus.DELETED),
                        user.email.notLike("deleted_%")
                )
                .execute();
    }

    @Override
    public void updateLastActiveAt(Long userId, LocalDateTime lastActiveAt) {
        queryFactory
                .update(user)
                .set(user.lastActiveAt, lastActiveAt)
                .where(user.id.eq(userId))
                .execute();
    }

    @Override
    public int suspendInactiveUsers(LocalDateTime threshold,
                                    UserStatus fromStatus,
                                    UserStatus toStatus) {
        return (int) queryFactory
                .update(user)
                .set(user.status, toStatus)
                .where(
                        user.status.eq(fromStatus),
                        user.lastActiveAt.isNull().or(user.lastActiveAt.lt(threshold))
                )
                .execute();
    }
}
