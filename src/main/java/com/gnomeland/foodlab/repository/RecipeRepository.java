package com.gnomeland.foodlab.repository;

import com.gnomeland.foodlab.model.Recipe;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Integer> {
    @Query("SELECT DISTINCT r FROM Recipe r "
            + "JOIN r.recipeIngredients ri "
            + "JOIN ri.ingredient i "
            +  "WHERE i.name LIKE %:ingredientName%")
    List<Recipe> findRecipesByIngredientName(@Param("ingredientName") String ingredientName);

    @Query(value = "SELECT DISTINCT r.* FROM recipes r "
            + "JOIN recipe_ingredients ri ON r.id = ri.recipe_id "
            + "JOIN ingredients i ON ri.ingredient_id = i.id "
            + "WHERE i.name LIKE :ingredientName", nativeQuery = true)
    List<Recipe> findRecipesByIngredientNameNative(@Param("ingredientName") String ingredientName);
}