package com.gnomeland.foodlab.repository;

import com.gnomeland.foodlab.model.Ingredient;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IngredientRepository extends JpaRepository<Ingredient, Integer> {
    List<Ingredient> findByNameIgnoreCase(String name);

    Optional<Ingredient> findByName(String name);
}
