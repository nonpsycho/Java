package com.gnomeland.foodlab.service;

import com.gnomeland.foodlab.cache.InMemoryCache;
import com.gnomeland.foodlab.dto.IngredientDto;
import com.gnomeland.foodlab.dto.RecipeIngredientDto;
import com.gnomeland.foodlab.exception.IngredientException;
import com.gnomeland.foodlab.exception.ValidationException;
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
    private static final int MIN_NAME_LENGTH = 2;
    private static final int MAX_NAME_LENGTH = 20;
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

        validateIngredientDto(ingredientDto, false);

        Ingredient ingredient = convertToEntity(ingredientDto);
        if (!ingredientRepository.findByNameIgnoreCase(ingredient.getName()).isEmpty()) {
            throw new IllegalArgumentException(INGREDIENT_ALREADY_EXISTS + ingredient.getName());
        }
        Ingredient savedIngredient = ingredientRepository.save(ingredient);

        return convertToDto(savedIngredient);
    }

    @Transactional
    public IngredientDto updateIngredient(Integer id, IngredientDto updatedIngredientDto) {

        validateIngredientDto(updatedIngredientDto, false);

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
        List<RecipeIngredient> recipeIngredients = recipeIngredientRepository
                .findByIngredientId(id);
        if (!recipeIngredients.isEmpty()) {
            throw new IngredientException(INGREDIENT_NOT_FOUND + id);
        }
        recipeIngredientRepository.deleteAll(recipeIngredients);

        inMemoryCache.removeAll();
        ingredientRepository.deleteById(id);

        return ResponseEntity.noContent().build();
    }


    public IngredientDto patchIngredient(Integer id, IngredientDto partialIngredientDto) {

        validateIngredientDto(partialIngredientDto, true);

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

    private void validateIngredientDto(IngredientDto ingredientDto, boolean isPartial) {
        if (ingredientDto == null) {
            throw new ValidationException("Ingredient data cannot be null");
        }

        if (isPartial) {
            validatePartialUpdate(ingredientDto);
        } else {
            validateFullDto(ingredientDto);
        }
    }

    private void validateFullDto(IngredientDto dto) {
        validateName(dto.getName());
        validateNutrient(dto.getProteins(), "Proteins");
        validateNutrient(dto.getFats(), "Fats");
        validateNutrient(dto.getCarbohydrates(), "Carbohydrates");
    }

    private void validatePartialUpdate(IngredientDto dto) {
        boolean hasValidFields = false;

        if (dto.getName() != null) {
            validateName(dto.getName());
            hasValidFields = true;
        }
        if (dto.getProteins() != null) {
            validateNutrient(dto.getProteins(), "Proteins");
            hasValidFields = true;
        }
        if (dto.getFats() != null) {
            validateNutrient(dto.getFats(), "Fats");
            hasValidFields = true;
        }
        if (dto.getCarbohydrates() != null) {
            validateNutrient(dto.getCarbohydrates(), "Carbohydrates");
            hasValidFields = true;
        }

        if (!hasValidFields) {
            throw new ValidationException("At least one valid field"
                    + " must be provided for partial update");
        }
    }

    private void validateName(String name) {
        if (isNullOrEmpty(name)) {
            throw new ValidationException("Name cannot be empty");
        }
        if (name.length() < MIN_NAME_LENGTH || name.length() > MAX_NAME_LENGTH) {
            throw new ValidationException(String.format(
                    "Name length must be between %d and %d characters",
                    MIN_NAME_LENGTH, MAX_NAME_LENGTH));
        }
    }

    private void validateNutrient(Double value, String fieldName) {
        if (value == null) {
            throw new ValidationException(fieldName + " cannot be null");
        }
        if (value < 0) {
            throw new ValidationException(fieldName + " cannot be negative");
        }
    }

    private boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
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
