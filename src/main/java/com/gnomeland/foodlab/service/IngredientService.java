package com.gnomeland.foodlab.service;

import com.gnomeland.foodlab.cache.InMemoryCache;
import com.gnomeland.foodlab.dto.IngredientDto;
import com.gnomeland.foodlab.dto.RecipeIngredientDto;
import com.gnomeland.foodlab.exception.IngredientNotFoundException;
import com.gnomeland.foodlab.model.Ingredient;
import com.gnomeland.foodlab.model.RecipeIngredient;
import com.gnomeland.foodlab.repository.IngredientRepository;
import com.gnomeland.foodlab.repository.RecipeIngredientRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class IngredientService {
    private static final String CACHE_KEY_RECIPE_INGREDIENT_PREFIX = "recipe_ingredient_";
    private final IngredientRepository ingredientRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final InMemoryCache inMemoryCache;

    @Autowired
    public IngredientService(IngredientRepository ingredientRepository,
                             RecipeIngredientRepository recipeIngredientRepository,
                             InMemoryCache inMemoryCache) {
        this.ingredientRepository = ingredientRepository;
        this.recipeIngredientRepository = recipeIngredientRepository;
        this.inMemoryCache = inMemoryCache;
    }

    public List<IngredientDto> getIngredients(String name) {
        List<Ingredient> ingredients;

        if (name != null) {
            ingredients = ingredientRepository.findByNameIgnoreCase(name);
        } else {
            ingredients = ingredientRepository.findAll();
        }

        return ingredients.stream().map(this::convertToDto).toList();
    }

    public IngredientDto getIngredientById(Integer id) {
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new IngredientNotFoundException(id));
        return convertToDto(ingredient);
    }

    public IngredientDto addIngredient(IngredientDto ingredientDto) {
        Optional<Ingredient> existingIngredient = ingredientRepository
                .findByName(ingredientDto.getName());
        if (existingIngredient.isPresent()) {
            throw new IllegalArgumentException("Ingredient with the same name already exist: "
                    + ingredientDto.getName());
        }

        Ingredient ingredient = convertToEntity(ingredientDto);
        return convertToDto(ingredientRepository.save(ingredient));
    }

    @Transactional
    public IngredientDto updateIngredient(Integer id, IngredientDto updatedIngredientDto) {
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new IngredientNotFoundException(id));

        final String oldName = ingredient.getName();

        ingredient.setName(updatedIngredientDto.getName());
        ingredient.setProteins(updatedIngredientDto.getProteins());
        ingredient.setFats(updatedIngredientDto.getFats());
        ingredient.setCarbohydrates(updatedIngredientDto.getCarbohydrates());

        Ingredient updatedIngredient = ingredientRepository.save(ingredient);

        inMemoryCache.remove(CACHE_KEY_RECIPE_INGREDIENT_PREFIX + oldName);
        inMemoryCache.remove(CACHE_KEY_RECIPE_INGREDIENT_PREFIX + updatedIngredient.getName());

        return convertToDto(updatedIngredient);
    }

    @Transactional
    public void deleteIngredientById(Integer id) {
        List<RecipeIngredient> recipeIngredients = recipeIngredientRepository
                .findByIngredientId(id);
        recipeIngredientRepository.deleteAll(recipeIngredients);

        inMemoryCache.removeAll();
        ingredientRepository.deleteById(id);

        ResponseEntity.noContent().build();
    }


    public IngredientDto patchIngredient(Integer id, IngredientDto partialIngredientDto) {
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new IngredientNotFoundException(id));

        final String oldName = ingredient.getName();

        if (partialIngredientDto.getName() != null) {
            ingredient.setName(partialIngredientDto.getName());
        }
        if (partialIngredientDto.getProteins() != null) {
            ingredient.setProteins(partialIngredientDto.getProteins());
        }
        if (partialIngredientDto.getFats() != null) {
            ingredient.setFats(partialIngredientDto.getFats());
        }
        if (partialIngredientDto.getCarbohydrates() != null) {
            ingredient.setCarbohydrates(partialIngredientDto.getCarbohydrates());
        }

        Ingredient updatedIngredient = ingredientRepository.save(ingredient);

        inMemoryCache.remove(CACHE_KEY_RECIPE_INGREDIENT_PREFIX + oldName);
        inMemoryCache.remove(CACHE_KEY_RECIPE_INGREDIENT_PREFIX + updatedIngredient.getName());

        return convertToDto(updatedIngredient);
    }

    public List<RecipeIngredientDto> getRecipesByIngredientId(Integer id) {
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new IngredientNotFoundException(id));

        return ingredient.getRecipeIngredients().stream()
                .map(this::convertToDto)
                .toList();
    }

    private IngredientDto convertToDto(Ingredient ingredient) {
        IngredientDto ingredientDto = new IngredientDto();
        ingredientDto.setId(ingredient.getId());
        ingredientDto.setName(ingredient.getName());
        ingredientDto.setProteins(ingredient.getProteins());
        ingredientDto.setFats(ingredient.getFats());
        ingredientDto.setCarbohydrates(ingredient.getCarbohydrates());

        // Добавляем информацию о связях с рецептами
        if (ingredient.getRecipeIngredients() != null) {
            List<RecipeIngredientDto> recipeIngredientDtos = ingredient
                    .getRecipeIngredients().stream()
                    .map(this::convertToDto)
                    .toList();
            ingredientDto.setRecipeIngredients(recipeIngredientDtos);
        }

        return ingredientDto;
    }

    private RecipeIngredientDto convertToDto(RecipeIngredient recipeIngredient) {
        RecipeIngredientDto dto = new RecipeIngredientDto();
        dto.setRecipeId(recipeIngredient.getRecipe().getId());
        dto.setIngredientId(recipeIngredient.getIngredient().getId());
        dto.setQuantityInGrams(recipeIngredient.getQuantityInGrams());
        return dto;
    }

    private Ingredient convertToEntity(IngredientDto ingredientDto) {
        Ingredient ingredient = new Ingredient();
        ingredient.setId(ingredientDto.getId());
        ingredient.setName(ingredientDto.getName());
        ingredient.setProteins(ingredientDto.getProteins());
        ingredient.setFats(ingredientDto.getFats());
        ingredient.setCarbohydrates(ingredientDto.getCarbohydrates());
        return ingredient;
    }
}
