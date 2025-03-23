package com.gnomeland.foodlab.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Duration;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
public class RecipeDto {
    private Integer id;
    private String name;
    private Duration preparationTime;
    private List<UserDto> users;
    private List<RecipeIngredientDto> recipeIngredients;
    private List<CommentDto> comments;
}

