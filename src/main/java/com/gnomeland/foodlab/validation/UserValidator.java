package com.gnomeland.foodlab.validation;

import com.gnomeland.foodlab.dto.UserDto;
import com.gnomeland.foodlab.exception.ValidationException;

public class UserValidator {
    private static final int MIN_NAME_LENGTH = 2;
    private static final int MAX_NAME_LENGTH = 20;
    private static final int MIN_PASSWORD_LENGTH = 5;
    private static final int MAX_PASSWORD_LENGTH = 20;

    private UserValidator() {
        throw new AssertionError("Cannot instantiate utility class");
    }

    public static void validateUserDto(UserDto userDto, boolean isPartial) {
        if (userDto == null) {
            throw new ValidationException("User data cannot be null");
        }

        if (isPartial) {
            validatePartialUpdate(userDto);
        } else {
            validateFullDto(userDto);
        }
    }

    private static void validateFullDto(UserDto dto) {
        validateUsername(dto.getUsername());
        validateEmail(dto.getEmail());
        validatePassword(dto.getPassword());
    }

    private static void validatePartialUpdate(UserDto dto) {
        boolean hasValidFields = false;

        if (dto.getUsername() != null) {
            validateUsername(dto.getUsername());
            hasValidFields = true;
        }
        if (dto.getEmail() != null) {
            validateEmail(dto.getEmail());
            hasValidFields = true;
        }
        if (dto.getPassword() != null) {
            validatePassword(dto.getPassword());
            hasValidFields = true;
        }

        if (!hasValidFields) {
            throw new ValidationException("At least one valid field "
                    + "must be provided for partial update");
        }
    }

    private static void validateUsername(String username) {
        if (isNullOrEmpty(username)) {
            throw new ValidationException("Username cannot be empty");
        }
        if (username.length() < MIN_NAME_LENGTH || username.length() > MAX_NAME_LENGTH) {
            throw new ValidationException(String.format(
                    "Username length must be between %d and %d characters",
                    MIN_NAME_LENGTH, MAX_NAME_LENGTH));
        }
    }

    private static void validateEmail(String email) {
        if (isNullOrEmpty(email)) {
            throw new ValidationException("Email cannot be empty");
        }
        if (!isValidEmailFormat(email)) {
            throw new ValidationException("Email format is not valid");
        }
    }

    private static void validatePassword(String password) {
        if (isNullOrEmpty(password)) {
            throw new ValidationException("Password cannot be empty");
        }
        if (password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
            throw new ValidationException(String.format(
                    "Password length must be between %d and %d characters",
                    MIN_PASSWORD_LENGTH, MAX_PASSWORD_LENGTH));
        }
    }

    private static boolean isValidEmailFormat(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return email.matches(emailRegex);
    }

    private static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}
