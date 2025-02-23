package com.gnomeland.foodlab.dao;

import com.gnomeland.foodlab.model.Recipe;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class RecipeDao {
    private final List<Recipe> recipes = new ArrayList<>();

    public RecipeDao() {
        recipes.add(new Recipe(1L, "Pasta", "Noodles, Sauce"));
        recipes.add(new Recipe(2L, "Salad", "Lettuce, Tomato, Cucumber"));
    }

    public List<Recipe> findAll() {
        return recipes;
    }

    public Optional<Recipe> findById(Long id) {
        return recipes.stream().filter(r -> r.id().equals(id)).findFirst();
    }

    public List<Recipe> findByName(String name) {
        return recipes.stream()
                .filter(r -> r.name().toLowerCase().contains(name.toLowerCase()))
                .toList();
    }
}
