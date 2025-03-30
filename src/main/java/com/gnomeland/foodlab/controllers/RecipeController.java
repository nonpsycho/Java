package com.gnomeland.foodlab.controllers;

import com.gnomeland.foodlab.dto.CommentDto;
import com.gnomeland.foodlab.dto.RecipeDto;
import com.gnomeland.foodlab.dto.RecipeIngredientDto;
import com.gnomeland.foodlab.dto.UserDto;
import com.gnomeland.foodlab.repository.RecipeRepository;
import com.gnomeland.foodlab.service.RecipeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Comment Controller", description = "API for managing recipes")
public class RecipeController {

    private final RecipeService recipeService;
    private final RecipeRepository recipeRepository;

    @Autowired
    public RecipeController(RecipeService recipeService, RecipeRepository recipeRepository) {
        this.recipeService = recipeService;
        this.recipeRepository = recipeRepository;
    }

    @Operation(summary = "Search for a recipe by filter",
            description = "Returns all recipes by name")
    @ApiResponses(value = { @ApiResponse (responseCode = "200",
            description = "The found recipes are returned"), @ApiResponse (responseCode = "404",
            description = "Recipes not found"),
    })
    @GetMapping
    public  ResponseEntity<List<RecipeDto>> getRecipes(
            @RequestParam(name = "name", required = false) final String name
    ) {
        List<RecipeDto> recipes = recipeService.getRecipes(name);
        return recipes.isEmpty() ? ResponseEntity.status(404).body(recipes)
                : ResponseEntity.ok(recipes);
    }

    @Operation(summary = "Getting a recipe by ID", description = "Returns the recipe by ID")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
            description = "The recipe is found"), @ApiResponse(responseCode = "404",
            description = "A recipe with this ID was not found.")
    })
    @GetMapping("/{id}")
    public ResponseEntity<RecipeDto> getRecipeById(@PathVariable final Integer id) {
        RecipeDto recipe = recipeService.getRecipeById(id);
        return ResponseEntity.ok(recipe);
    }

    @Operation(summary = "Getting comments for a recipe by its ID",
            description = "Returns comments for a recipe with an ID")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
            description = "Comments returned"), @ApiResponse(responseCode = "404",
            description = "A recipe with this ID was not found.")
    })
    @GetMapping("/{id}/comments")
    public ResponseEntity<List<CommentDto>> getCommentsByRecipeId(@PathVariable final Integer id) {
        List<CommentDto> comments = recipeService.getCommentsByRecipeId(id);
        return ResponseEntity.ok(comments);
    }

    @Operation(summary = "Adding a new recipe", description = "Creates a new recipe")
    @ApiResponses(value = { @ApiResponse(responseCode = "201",
            description = "The recipe was added successfully"), @ApiResponse(responseCode = "400",
            description = "Invalid request"), @ApiResponse(responseCode = "404",
            description = "Such a recipe already exists")
    })
    @PostMapping
    public ResponseEntity<RecipeDto> addRecipe(@RequestBody RecipeDto recipeDto) {
        RecipeDto newRecipe = recipeService.addRecipe(recipeDto);
        return ResponseEntity.status(201).body(newRecipe);
    }

    @Operation(summary = "Deleting a recipe by its ID", description = "Delete a recipe with an ID")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
            description = "The recipe was deleted successfully"), @ApiResponse(responseCode = "404",
            description = "A recipe with this ID was not found.")
    })
    @DeleteMapping("/{id}")
    public void deleteRecipe(@PathVariable Integer id) {
        recipeService.deleteRecipeById(id);
    }

    @Operation(summary = "Changing the recipe",
            description = "Changes all information about the recipe")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
            description = "The recipe was changed successfully"), @ApiResponse(responseCode = "400",
            description = "Invalid request"),
    })
    @PutMapping("/{id}")
    public ResponseEntity<RecipeDto> updateRecipe(@PathVariable Integer id,
                                  @RequestBody RecipeDto updatedRecipeDto) {
        RecipeDto updateRecipe = recipeService.updateRecipe(id, updatedRecipeDto);
        return ResponseEntity.status(200).body(updateRecipe);
    }

    @Operation(summary = "Changing the recipe",
            description = "Changes information about the recipe")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
            description = "The recipe was changed successfully"), @ApiResponse(responseCode = "400",
            description = "Invalid request"),
    })
    @PatchMapping("/{id}")
    public ResponseEntity<RecipeDto> patchRecipe(@PathVariable Integer id,
                                 @RequestBody RecipeDto partialRecipeDto) {
        RecipeDto patchedRecipe = recipeService.patchRecipe(id, partialRecipeDto);
        return ResponseEntity.ok(patchedRecipe);
    }

    @Operation(summary = "Adding a user to a recipe",
            description = "Connects the user and the recipe")
    @ApiResponses(value = { @ApiResponse(responseCode = "201",
            description = "The user was added successfully"), @ApiResponse(responseCode = "404",
            description = "The recipe already has such a user."),
    })
    @PostMapping("/{recipeId}/users/{userId}")
    public ResponseEntity<String> addUserToRecipe(@PathVariable Integer recipeId,
                                                  @PathVariable Integer userId) {
        return recipeService.addUserToRecipe(recipeId, userId);
    }

    @Operation(summary = "Removing a user from a recipe",
            description = "Removes the connection between the user and the recipe")
    @ApiResponses(value = { @ApiResponse(responseCode = "204",
            description = "The user was deleted successfully"), @ApiResponse(responseCode = "404",
            description = "The user or recipe was not found")
    })
    @DeleteMapping("/{recipeId}/users/{userId}")
    public ResponseEntity<Void> removeUserFromRecipe(@PathVariable Integer recipeId,
                                                     @PathVariable Integer userId) {
        return recipeService.removeUserFromRecipe(recipeId, userId);
    }

    @Operation(summary = "Search for all users for a recipe",
            description = "Returns all users associated with the recipe")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
            description = "The list of users has been returned"), @ApiResponse(responseCode = "404",
            description = "The recipe was not found"),
    })
    @GetMapping("/{recipeId}/users")
    public ResponseEntity<List<UserDto>> getUsersForRecipe(@PathVariable Integer recipeId) {
        List<UserDto> users = recipeService.getUsersForRecipe(recipeId);
        return ResponseEntity.ok(users);
    }

    @Operation(summary = "Search recipes by ingredient (JPA)",
            description = "Returns recipes containing specified ingredient using JPA query")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
            description = "Recipes found successfully"), @ApiResponse(responseCode = "204",
            description = "No recipes found with this ingredient"),
        @ApiResponse(responseCode = "400", description = "Invalid ingredient name format")
    })
    @GetMapping("/recipe-by-ingredient")
    public ResponseEntity<List<RecipeDto>> searchRecipesByIngredient(
            @RequestParam String ingredientName) {
        List<RecipeDto> recipes = recipeService.getRecipesByIngredientFromCacheOrDb(ingredientName,
                recipeRepository::findRecipesByIngredientName);
        return recipes.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(recipes);
    }

    @Operation(summary = "Search recipes by ingredient (Native SQL)",
            description = "Returns recipes containing specified ingredient using native SQL query")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
            description = "Recipes found successfully"), @ApiResponse(responseCode = "204",
            description = "No recipes found with this ingredient"),
        @ApiResponse(responseCode = "400", description = "Invalid ingredient name format")
    })
    @GetMapping("/recipe-by-ingredient-native")
    public ResponseEntity<List<RecipeDto>> searchRecipesByIngredientNative(
            @RequestParam String ingredientName) {
        List<RecipeDto> recipes = recipeService.getRecipesByIngredientFromCacheOrDb(ingredientName,
                recipeRepository::findRecipesByIngredientNameNative);
        return recipes.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(recipes);
    }

    @Operation(summary = "Adding a ingredient to a recipe",
            description = "Connects the ingredient and the recipe")
    @ApiResponses(value = { @ApiResponse(responseCode = "201",
            description = "The ingredient was added successfully"),
        @ApiResponse(responseCode = "404",
                description = "The recipe already has such a ingredient."),
    })
    @PostMapping("/{recipeId}/ingredients/{ingredientId}")
    public ResponseEntity<String> addIngredientToRecipe(
            @PathVariable Integer recipeId,
            @PathVariable Integer ingredientId,
            @RequestParam Double quantityInGrams) {
        return recipeService.addIngredientToRecipe(recipeId, ingredientId, quantityInGrams);
    }

    @Operation(summary = "Removing a ingredient from a recipe",
            description = "Removes the connection between the ingredient and the recipe")
    @ApiResponses(value = { @ApiResponse(responseCode = "204",
            description = "The ingredient was deleted successfully"),
        @ApiResponse(responseCode = "404",
            description = "The ingredient or recipe was not found")
    })
    @DeleteMapping("/{recipeId}/ingredients/{ingredientId}")
    public ResponseEntity<Void> removeIngredientFromRecipe(
            @PathVariable Integer recipeId,
            @PathVariable Integer ingredientId) {
        return recipeService.removeIngredientFromRecipe(recipeId, ingredientId);
    }

    @Operation(summary = "Search for all ingredients for a recipe",
            description = "Returns all ingredients associated with the recipe")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
            description = "The list of ingredients has been returned"),
        @ApiResponse(responseCode = "404", description = "The recipe was not found"),
    })
    @GetMapping("/{recipeId}/ingredients")
    public ResponseEntity<List<RecipeIngredientDto>> getIngredientsForRecipe(@PathVariable
                                                                                 Integer recipeId) {
        List<RecipeIngredientDto> ingredients = recipeService.getIngredientsForRecipe(recipeId);
        return ResponseEntity.ok(ingredients);
    }
}
