package com.gnomeland.foodlab.validation;

import com.gnomeland.foodlab.dto.RecipeDto;
import com.gnomeland.foodlab.exception.ValidationException;
import java.time.Duration;

public class RecipeValidator {
    private RecipeValidator() {
        throw new AssertionError("Cannot instantiate utility class");
    }

    public static void validateRecipeDto(RecipeDto recipeDto, boolean isPartial) {
        if (recipeDto == null) {
            throw new ValidationException("Recipe cannot be null");
        }

        if (!isPartial) {
            validateMandatoryFields(recipeDto);
            validateName(recipeDto.getName());
            validateDuration(recipeDto.getPreparationTime());
        } else {
            if (recipeDto.getName() != null) {
                validateName(recipeDto.getName());
            }
            if (recipeDto.getPreparationTime() != null) {
                validateDuration(recipeDto.getPreparationTime());
            }
        }
    }

    private static void validateMandatoryFields(RecipeDto recipeDto) {
        if (isNullOrEmpty(recipeDto.getName())) {
            throw new ValidationException("Name is required");
        }
        if (recipeDto.getPreparationTime() == null) {
            throw new ValidationException("Preparation time is required");
        }
    }

    private static void validateName(String name) {
        if (isNullOrEmpty(name)) {
            throw new ValidationException("Name cannot be empty");
        }
        if (name.length() > 50) {
            throw new ValidationException("Name cannot exceed 50 characters");
        }
    }

    private static void validateDuration(Duration duration) {
        if (duration.isNegative()) {
            throw new ValidationException("Preparation time cannot be negative");
        }
        if (duration.toMinutes() < 1) {
            throw new ValidationException("Preparation time must be at least 1 minute");
        }
        if (duration.toHours() > 24) {
            throw new ValidationException("Preparation time cannot exceed 24 hours");
        }
    }

    private static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}