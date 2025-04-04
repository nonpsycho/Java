package com.gnomeland.foodlab.exception;

import lombok.Getter;

@Getter
public class RecipeException extends RuntimeException {
    private final String errorCode;

    public RecipeException(String message) {
        super(message);
        this.errorCode = "400";
    }
}

