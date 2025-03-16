package com.gnomeland.foodlab.controllers;

import com.gnomeland.foodlab.dto.CommentDto;
import com.gnomeland.foodlab.dto.RecipeDto;
import com.gnomeland.foodlab.dto.UserDto;
import com.gnomeland.foodlab.service.RecipeService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private final RecipeService recipeService;

    @Autowired
    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping
    public List<RecipeDto> getRecipes(
            @RequestParam(name = "name", required = false) final String name
    ) {
        return recipeService.getRecipes(name);
    }

    @GetMapping("/{id}")
    public RecipeDto getRecipeById(@PathVariable final Integer id) {
        return recipeService.getRecipeById(id);
    }

    @GetMapping("/{id}/comments")
    public List<CommentDto> getCommentsByRecipeId(@PathVariable final Integer id) {
        return recipeService.getCommentsByRecipeId(id);
    }

    @PostMapping
    public RecipeDto addRecipe(@RequestBody RecipeDto recipeDto) {
        return recipeService.addRecipe(recipeDto);
    }

    @DeleteMapping("/{id}")
    public void deleteRecipe(@PathVariable Integer id) {
        recipeService.deleteRecipeById(id);
    }

    @PutMapping("/{id}")
    public RecipeDto updateRecipe(@PathVariable Integer id,
                                  @RequestBody RecipeDto updatedRecipeDto) {
        return recipeService.updateRecipe(id, updatedRecipeDto);
    }

    @PatchMapping("/{id}")
    public RecipeDto patchMovie(@PathVariable Integer id, @RequestBody RecipeDto partialRecipeDto) {
        return recipeService.patchRecipe(id, partialRecipeDto);
    }

    @PostMapping("/{recipeId}/users/{userId}")
    public ResponseEntity<String> addUserToRecipe(@PathVariable Integer recipeId,
                                                  @PathVariable Integer userId) {
        return recipeService.addUserToRecipe(recipeId, userId);
    }

    @DeleteMapping("/{recipeId}/users/{userId}")
    public ResponseEntity<Void> removeUserFromRecipe(@PathVariable Integer recipeId,
                                                     @PathVariable Integer userId) {
        return recipeService.removeUserFromRecipe(recipeId, userId);
    }

    @GetMapping("/{recipeId}/users")
    public ResponseEntity<List<UserDto>> getUsersForMovie(@PathVariable Integer recipeId) {
        return ResponseEntity.ok(recipeService.getUsersForRecipe(recipeId));
    }
}