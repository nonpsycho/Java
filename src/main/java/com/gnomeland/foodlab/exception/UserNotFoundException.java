package com.gnomeland.foodlab.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Integer userId) {
        super("User with id " + userId + " not found");
    }
}
