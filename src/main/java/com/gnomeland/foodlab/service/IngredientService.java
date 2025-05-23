package com.gnomeland.foodlab.service;

import com.gnomeland.foodlab.cache.InMemoryCache;
import com.gnomeland.foodlab.dto.IngredientDto;
import com.gnomeland.foodlab.dto.RecipeIngredientDto;
import com.gnomeland.foodlab.exception.IngredientException;
import com.gnomeland.foodlab.model.Ingredient;
import com.gnomeland.foodlab.model.RecipeIngredient;
import com.gnomeland.foodlab.repository.IngredientRepository;
import com.gnomeland.foodlab.repository.RecipeIngredientRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class IngredientService {
    private static final String INGREDIENT_NOT_FOUND = "Ingredient not found: ";
    private static final String INGREDIENT_ALREADY_EXISTS = "Ingredient already exists: ";
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

        if (ingredients.isEmpty()) {
            throw new IngredientException(INGREDIENT_NOT_FOUND + name);
        }

        return ingredients.stream().map(this::convertToDto).toList();
    }

    public IngredientDto getIngredientById(Integer id) {
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new IngredientException(INGREDIENT_NOT_FOUND + id));
        return convertToDto(ingredient);
    }

    public IngredientDto addIngredient(IngredientDto ingredientDto) {
        Ingredient ingredient = convertToEntity(ingredientDto);
        if (!ingredientRepository.findByNameIgnoreCase(ingredient.getName()).isEmpty()) {
            throw new IllegalArgumentException(INGREDIENT_ALREADY_EXISTS + ingredient.getName());
        }
        Ingredient savedIngredient = ingredientRepository.save(ingredient);

        return convertToDto(savedIngredient);
    }

    @Transactional
    public IngredientDto updateIngredient(Integer id, IngredientDto updatedIngredientDto) {
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new IngredientException(INGREDIENT_NOT_FOUND + id));

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
    public ResponseEntity<String> deleteIngredientById(Integer id) {
        // Проверяем существование ингредиента
        if (!ingredientRepository.existsById(id)) {
            throw new IngredientException(INGREDIENT_NOT_FOUND + id);
        }

        // Проверяем наличие связанных рецептов
        List<RecipeIngredient> recipeIngredients = recipeIngredientRepository
                .findByIngredientId(id);
        if (!recipeIngredients.isEmpty()) {
            throw new IngredientException("Cannot delete ingredient with existing recipes");
        }

        // Очищаем кэш и удаляем ингредиент
        inMemoryCache.removeAll();
        ingredientRepository.deleteById(id);

        return ResponseEntity.noContent().build();
    }


    public IngredientDto patchIngredient(Integer id, IngredientDto partialIngredientDto) {
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new IngredientException(INGREDIENT_NOT_FOUND + id));

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
                .orElseThrow(() -> new IngredientException(INGREDIENT_NOT_FOUND + id));

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
