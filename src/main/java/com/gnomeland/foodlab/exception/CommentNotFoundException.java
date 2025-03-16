package com.gnomeland.foodlab.exception;

public class CommentNotFoundException extends RuntimeException {
    public CommentNotFoundException(Integer commentId) {
        super("Could not find comment with id " + commentId);
    }
}
