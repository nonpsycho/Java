package com.gnomeland.foodlab.exception;

public class IngredientNotFoundException extends RuntimeException {
    public IngredientNotFoundException(Integer ingredientId) {
        super("Ingredient with id " + ingredientId + " not found");
    }
}
