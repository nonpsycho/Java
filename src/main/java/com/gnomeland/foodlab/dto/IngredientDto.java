package com.gnomeland.foodlab.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
public class IngredientDto {
    private Integer id;
    private String name;
    private Double proteins;
    private Double fats;
    private Double carbohydrates;
    private List<RecipeIngredientDto> recipeIngredients;
}
