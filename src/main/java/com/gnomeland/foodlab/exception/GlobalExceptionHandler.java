package com.gnomeland.foodlab.exception;

import com.gnomeland.foodlab.dto.CommentDto;
import com.gnomeland.foodlab.dto.IngredientDto;
import com.gnomeland.foodlab.dto.RecipeDto;
import com.gnomeland.foodlab.dto.UserDto;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserAssociatedException.class)
    public ResponseEntity<String> handleUserAssociatedException(
            UserAssociatedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<String> handleValidationException(ValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(RecipeException.class)
    public ResponseEntity<RecipeDto> handleMovieException(RecipeException e) {
        RecipeDto errorDto = new RecipeDto();
        errorDto.setName(e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorDto); // 404
    }

    @ExceptionHandler(CommentException.class)
    public ResponseEntity<CommentDto> handleCommentException(CommentException e) {
        CommentDto errorDto = new CommentDto();
        errorDto.setText(e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorDto); // 404
    }

    @ExceptionHandler(UserException.class)
    public ResponseEntity<UserDto> handleUserException(UserException e) {
        UserDto errorDto = new UserDto();
        errorDto.setUsername(e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorDto);
    }

    @ExceptionHandler(IngredientException.class)
    public ResponseEntity<IngredientDto> handleIngredientException(IngredientException e) {
        IngredientDto errorDto = new IngredientDto();
        errorDto.setName(e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorDto);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        return buildErrorResponse("Internal server error: " + e.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            String message, HttpStatus status) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("message", message);
        return new ResponseEntity<>(errorResponse, status);
    }
}
