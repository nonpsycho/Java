package com.gnomeland.foodlab.validation;

import com.gnomeland.foodlab.dto.CommentDto;
import com.gnomeland.foodlab.exception.ValidationException;

public class CommentValidator {
    private static final int MAX_COMMENT_LENGTH = 300;
    private static final String COMMENT_NOT_BLANK_MESSAGE = "Comment cannot be empty";
    private static final String COMMENT_SIZE_MESSAGE =
            "Comment length must be between 1 and " + MAX_COMMENT_LENGTH + " characters";

    private CommentValidator() {
        throw new AssertionError("Cannot instantiate utility class");
    }

    public static void validateCommentDto(CommentDto commentDto) {
        if (commentDto == null) {
            throw new ValidationException("Comment data cannot be null");
        }

        validateText(commentDto.getText());
    }

    public static void validateText(String text) {
        if (isNullOrEmpty(text)) {
            throw new ValidationException(COMMENT_NOT_BLANK_MESSAGE);
        }
        if (text.length() > MAX_COMMENT_LENGTH) {
            throw new ValidationException(COMMENT_SIZE_MESSAGE);
        }
    }

    private static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}