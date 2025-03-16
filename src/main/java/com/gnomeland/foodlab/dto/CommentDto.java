package com.gnomeland.foodlab.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
public class CommentDto {
    private Integer id;
    private String text;
    private Integer userId;
    private Integer recipeId;
}