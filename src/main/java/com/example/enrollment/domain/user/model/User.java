package com.example.enrollment.domain.user.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class User {

    private final Long id;
    private final String name;
    private final Role role;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public enum Role {
        CREATOR, STUDENT
    }

    public boolean isCreator() {
        return this.role == Role.CREATOR;
    }

    public boolean isStudent() {
        return this.role == Role.STUDENT;
    }
}