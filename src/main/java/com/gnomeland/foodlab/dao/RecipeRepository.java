package com.gnomeland.foodlab.dao;

import com.gnomeland.foodlab.model.Recipe;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Integer> {

    List<Recipe> findByNameIgnoreCase(String name);

}
