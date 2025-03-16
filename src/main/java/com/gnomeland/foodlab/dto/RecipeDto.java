package com.gnomeland.foodlab.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
public class RecipeDto {
    private Integer id;
    private String name;
    private String ingredients;
    private List<UserDto> users;
    private List<CommentDto> comments;
}