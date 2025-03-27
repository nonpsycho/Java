package com.gnomeland.foodlab.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecipeIngredientDto {
    private Integer id;
    private Integer recipeId;
    private Integer ingredientId;
    private IngredientDto ingredient;
    private Double quantityInGrams;
}

