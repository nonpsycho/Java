package com.gnomeland.foodlab.exception;

public class RecipeNotFoundException extends RuntimeException {
    public RecipeNotFoundException(Integer recipeId) {
        super("Recipe with id " + recipeId + " not found");
    }
}
