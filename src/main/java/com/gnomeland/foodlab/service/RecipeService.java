package com.gnomeland.foodlab.service;

import com.gnomeland.foodlab.dao.RecipeDao;
import com.gnomeland.foodlab.exception.RecipeException;
import com.gnomeland.foodlab.model.Recipe;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RecipeService {

    private final RecipeDao recipeDao;

    public RecipeService(RecipeDao recipeDao) {
        this.recipeDao = recipeDao;
    }

    public List<Recipe> getAllRecipes() {
        return recipeDao.findAll();
    }

    public Recipe getRecipeById(Long id) {
        return recipeDao.findById(id)
                .orElseThrow(() -> new RecipeException("Recipe with id: " + id + " not found."));
    }

    public List<Recipe> getRecipesByName(String name) {
        List<Recipe> results = recipeDao.findByName(name);
        if (results.isEmpty()) {
            throw new RecipeException("Recipe with name: " + name + " not found.");
        }
        return results;
    }
}
