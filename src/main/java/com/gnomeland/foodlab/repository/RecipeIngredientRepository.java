package com.gnomeland.foodlab.repository;

import com.gnomeland.foodlab.model.RecipeIngredient;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;


public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, Integer> {
    List<RecipeIngredient> findByIngredientId(Integer ingredientId);
}