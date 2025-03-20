package com.gnomeland.foodlab.controllers;

import com.gnomeland.foodlab.dto.IngredientDto;
import com.gnomeland.foodlab.dto.RecipeIngredientDto;
import com.gnomeland.foodlab.service.IngredientService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
public class IngredientController {

    private final IngredientService ingredientService;

    @Autowired
    public IngredientController(IngredientService ingredientService) {
        this.ingredientService = ingredientService;
    }

    @GetMapping
    public List<IngredientDto> getIngredients(
            @RequestParam(name = "name", required = false) final String name
    ) {
        return ingredientService.getIngredients(name);
    }

    @GetMapping("/{id}")
    public ResponseEntity<IngredientDto> getIngredientById(@PathVariable Integer id) {
        IngredientDto ingredient = ingredientService.getIngredientById(id);
        return new ResponseEntity<>(ingredient, HttpStatus.OK);
    }

    @GetMapping("/{id}/recipes")
    public ResponseEntity<List<RecipeIngredientDto>> getRecipesByIngredientId(@PathVariable
                                                                                  Integer id) {
        List<RecipeIngredientDto> recipes = ingredientService.getRecipesByIngredientId(id);
        return new ResponseEntity<>(recipes, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<IngredientDto> createIngredient(@RequestBody
                                                              IngredientDto ingredientDto) {
        IngredientDto createdIngredient = ingredientService.addIngredient(ingredientDto);
        return new ResponseEntity<>(createdIngredient, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<IngredientDto> updateIngredient(
            @PathVariable Integer id,
            @RequestBody IngredientDto ingredientDto) {
        IngredientDto updatedIngredient = ingredientService.updateIngredient(id, ingredientDto);
        return new ResponseEntity<>(updatedIngredient, HttpStatus.OK);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<IngredientDto> patchIngredient(
            @PathVariable Integer id,
            @RequestBody IngredientDto partialIngredientDto) {
        IngredientDto updatedIngredient = ingredientService
                .patchIngredient(id, partialIngredientDto);
        return new ResponseEntity<>(updatedIngredient, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIngredient(@PathVariable Integer id) {
        ingredientService.deleteIngredientById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}