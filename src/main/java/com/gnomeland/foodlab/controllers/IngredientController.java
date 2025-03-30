package com.gnomeland.foodlab.controllers;

import com.gnomeland.foodlab.dto.IngredientDto;
import com.gnomeland.foodlab.dto.RecipeIngredientDto;
import com.gnomeland.foodlab.service.IngredientService;
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
@RequestMapping("/api/ingredients")
@Tag(name = "Ingredient Controller", description = "API for ingredient management")
public class IngredientController {

    private final IngredientService ingredientService;

    @Autowired
    public IngredientController(IngredientService ingredientService) {
        this.ingredientService = ingredientService;
    }

    @Operation(summary = "Ingredient search by filter",
            description = "Returns all ingredients filtered by name if provided")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
            description = "The found ingredients are returned"), @ApiResponse(responseCode = "404",
            description = "No ingredients found"),
    })
    @GetMapping
    public ResponseEntity<List<IngredientDto>> getIngredients(
            @RequestParam(name = "name", required = false) final String name
    ) {
        List<IngredientDto> ingredients = ingredientService.getIngredients(name);
        return ResponseEntity.ok(ingredients);
    }

    @Operation(summary = "Search for an ingredient by ID",
            description = "Returns the ingredient by its ID")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
            description = "The ingredient has been found"), @ApiResponse(responseCode = "404",
            description = "There is no such ingredient")
    })
    @GetMapping("/{id}")
    public ResponseEntity<IngredientDto> getIngredientById(@PathVariable Integer id) {
        IngredientDto ingredient = ingredientService.getIngredientById(id);
        return ResponseEntity.ok(ingredient);
    }

    @Operation(summary = "Getting recipes by ingredient ID",
            description = "Returns all recipes that use the ingredient with the specified ID")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Recipes returned"),
        @ApiResponse(responseCode = "404",
                description = "The ingredient with this ID was not found")
    })
    @GetMapping("/{id}/recipes")
    public ResponseEntity<List<RecipeIngredientDto>> getRecipesByIngredientId(@PathVariable
                                                                                  Integer id) {
        List<RecipeIngredientDto> recipes = ingredientService.getRecipesByIngredientId(id);
        return ResponseEntity.ok(recipes);
    }

    @Operation(summary = "Adding a new ingredient", description = "Creates a new ingredient")
    @ApiResponses(value = {@ApiResponse(responseCode = "201",
            description = "The ingredient was created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "409", description = "Such ingredient already exists")
    })
    @PostMapping
    public ResponseEntity<IngredientDto> createIngredient(@RequestBody
                                                              IngredientDto ingredientDto) {
        IngredientDto createdIngredient = ingredientService.addIngredient(ingredientDto);
        return ResponseEntity.status(201).body(createdIngredient);
    }

    @Operation(summary = "Updating an ingredient",
            description = "Fully updates all ingredient information")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
            description = "The ingredient was updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "404", description = "Ingredient not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<IngredientDto> updateIngredient(
            @PathVariable Integer id,
            @RequestBody IngredientDto ingredientDto) {
        IngredientDto updatedIngredient = ingredientService.updateIngredient(id, ingredientDto);
        return ResponseEntity.ok(updatedIngredient);
    }

    @Operation(summary = "Partially updating an ingredient",
            description = "Updates only the provided fields of the ingredient")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
            description = "The ingredient was updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "404", description = "Ingredient not found")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<IngredientDto> patchIngredient(
            @PathVariable Integer id,
            @RequestBody IngredientDto partialIngredientDto) {
        IngredientDto updatedIngredient = ingredientService
                .patchIngredient(id, partialIngredientDto);
        return ResponseEntity.ok(updatedIngredient);
    }

    @Operation(summary = "Deleting an ingredient",
            description = "Deletes an ingredient by its ID")
    @ApiResponses(value = { @ApiResponse(responseCode = "204",
            description = "The ingredient was deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Ingredient not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteIngredient(@PathVariable Integer id) {
        return ingredientService.deleteIngredientById(id);
    }
}
