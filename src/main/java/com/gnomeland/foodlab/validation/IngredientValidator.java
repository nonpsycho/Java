package com.gnomeland.foodlab.validation;

import com.gnomeland.foodlab.dto.IngredientDto;
import com.gnomeland.foodlab.exception.ValidationException;

public class IngredientValidator {
    private static final int MIN_NAME_LENGTH = 2;
    private static final int MAX_NAME_LENGTH = 20;

    private IngredientValidator() {
        throw new AssertionError("Cannot instantiate utility class");
    }

    public static void validateIngredientDto(IngredientDto ingredientDto, boolean isPartial) {
        if (ingredientDto == null) {
            throw new ValidationException("Ingredient data cannot be null");
        }

        if (isPartial) {
            validatePartialUpdate(ingredientDto);
        } else {
            validateFullDto(ingredientDto);
        }
    }

    private static void validateFullDto(IngredientDto dto) {
        validateName(dto.getName());
        validateNutrient(dto.getProteins(), "Proteins");
        validateNutrient(dto.getFats(), "Fats");
        validateNutrient(dto.getCarbohydrates(), "Carbohydrates");
    }

    private static void validatePartialUpdate(IngredientDto dto) {
        boolean hasValidFields = false;

        if (dto.getName() != null) {
            validateName(dto.getName());
            hasValidFields = true;
        }
        if (dto.getProteins() != null) {
            validateNutrient(dto.getProteins(), "Proteins");
            hasValidFields = true;
        }
        if (dto.getFats() != null) {
            validateNutrient(dto.getFats(), "Fats");
            hasValidFields = true;
        }
        if (dto.getCarbohydrates() != null) {
            validateNutrient(dto.getCarbohydrates(), "Carbohydrates");
            hasValidFields = true;
        }

        if (!hasValidFields) {
            throw new ValidationException("At least one valid field"
                    + " must be provided for partial update");
        }
    }

    private static void validateName(String name) {
        if (isNullOrEmpty(name)) {
            throw new ValidationException("Name cannot be empty");
        }
        if (name.length() < MIN_NAME_LENGTH || name.length() > MAX_NAME_LENGTH) {
            throw new ValidationException(String.format(
                    "Name length must be between %d and %d characters",
                    MIN_NAME_LENGTH, MAX_NAME_LENGTH));
        }
    }

    private static void validateNutrient(Double value, String fieldName) {
        if (value == null) {
            throw new ValidationException(fieldName + " cannot be null");
        }
        if (value < 0) {
            throw new ValidationException(fieldName + " cannot be negative");
        }
    }

    private static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}