package com.gnomeland.foodlab.service;

import com.gnomeland.foodlab.cache.InMemoryCache;
import com.gnomeland.foodlab.dto.CommentDto;
import com.gnomeland.foodlab.dto.IngredientDto;
import com.gnomeland.foodlab.dto.RecipeDto;
import com.gnomeland.foodlab.dto.RecipeIngredientDto;
import com.gnomeland.foodlab.dto.UserDto;
import com.gnomeland.foodlab.exception.IngredientAssociatedException;
import com.gnomeland.foodlab.exception.IngredientException;
import com.gnomeland.foodlab.exception.RecipeException;
import com.gnomeland.foodlab.exception.UserAssociatedException;
import com.gnomeland.foodlab.exception.UserException;
import com.gnomeland.foodlab.model.Comment;
import com.gnomeland.foodlab.model.Ingredient;
import com.gnomeland.foodlab.model.Recipe;
import com.gnomeland.foodlab.model.RecipeIngredient;
import com.gnomeland.foodlab.model.User;
import com.gnomeland.foodlab.repository.IngredientRepository;
import com.gnomeland.foodlab.repository.RecipeRepository;
import com.gnomeland.foodlab.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;


@Service
public class RecipeService {
    private static final String RECIPE_NOT_FOUND_MESSAGE = "The recipe was not found: ";
    private static final String USER_NOT_FOUND_MESSAGE = "The user was not found: ";
    private static final String INGREDIENT_NOT_FOUND_MESSAGE = "The ingredient was not found: ";
    private static final String CACHE_KEY = "recipe_ingredient_";
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final IngredientRepository ingredientRepository;
    private final InMemoryCache inMemoryCache;
    private static final Logger logger = LoggerFactory.getLogger(RecipeService.class);

    @Autowired
    public RecipeService(RecipeRepository recipeRepository, UserRepository userRepository,
                         IngredientRepository ingredientRepository, InMemoryCache inMemoryCache) {
        this.recipeRepository = recipeRepository;
        this.userRepository = userRepository;
        this.ingredientRepository = ingredientRepository;
        this.inMemoryCache = inMemoryCache;
    }

    public List<RecipeDto> getRecipes(String name) {
        List<RecipeDto> recipes =  recipeRepository.findAll().stream()
                .filter(recipe -> name == null || recipe.getName().equalsIgnoreCase(name))
                .map(this::convertToDto)
                .toList();
        if (recipes.isEmpty()) {
            throw new RecipeException(RECIPE_NOT_FOUND_MESSAGE + name);
        }

        return recipes;
    }

    public RecipeDto getRecipeById(Integer id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new RecipeException(RECIPE_NOT_FOUND_MESSAGE + id));
        return convertToDto(recipe);
    }

    @Transactional
    public RecipeDto addRecipe(RecipeDto recipeDto) {
        Recipe recipe = convertToEntity(recipeDto);
        return convertToDto(recipeRepository.save(recipe));
    }

    @Transactional
    public List<RecipeDto> addRecipesBulk(List<RecipeDto> recipeDtos) {
        return recipeDtos.stream()
                .map(this::convertToEntity)
                .map(recipeRepository::save)
                .map(this::convertToDto)
                .toList();
    }

    @Transactional
    public void deleteRecipeById(Integer id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new RecipeException(RECIPE_NOT_FOUND_MESSAGE + id));

        for (RecipeIngredient recipeIngredient : recipe.getRecipeIngredients()) {
            Ingredient ingredient = recipeIngredient.getIngredient();

            String key = CACHE_KEY + ingredient.getName();
            inMemoryCache.remove(key);

            ingredient.getRecipeIngredients().removeIf(ri -> ri.getRecipe().getId().equals(id));
            ingredientRepository.save(ingredient);
        }

        for (User user : recipe.getUsers()) {
            user.getSavedRecipes().removeIf(r -> r.getId().equals(id));
            userRepository.save(user);
        }

        recipeRepository.deleteById(id);

        ResponseEntity.noContent().build();
    }

    @Transactional
    public RecipeDto updateRecipe(Integer id, RecipeDto updatedRecipeDto) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new RecipeException(RECIPE_NOT_FOUND_MESSAGE + id));

        // Обновляем основные поля
        recipe.setName(updatedRecipeDto.getName());
        recipe.setPreparationTime(updatedRecipeDto.getPreparationTime());

        // Создаем Map для новых ингредиентов
        Map<Integer, Double> newIngredients = updatedRecipeDto.getRecipeIngredients().stream()
                .collect(Collectors.toMap(
                        RecipeIngredientDto::getIngredientId,
                        RecipeIngredientDto::getQuantityInGrams));

        // 1. Обновляем существующие ингредиенты
        recipe.getRecipeIngredients().removeIf(ri -> {
            if (newIngredients.containsKey(ri.getIngredient().getId())) {
                ri.setQuantityInGrams(newIngredients.get(ri.getIngredient().getId()));
                newIngredients.remove(ri.getIngredient().getId());
                return false;
            }
            return true;
        });

        newIngredients.forEach((ingId, grams) -> {
            RecipeIngredient ri = new RecipeIngredient();
            ri.setRecipe(recipe);
            ri.setIngredient(ingredientRepository.findById(ingId)
                    .orElseThrow(() -> new IngredientException(INGREDIENT_NOT_FOUND_MESSAGE + id)));
            ri.setQuantityInGrams(grams);
            recipe.getRecipeIngredients().add(ri);
        });

        Recipe updatedRecipe = recipeRepository.save(recipe);

        for (RecipeIngredient recipeIngredient : recipe.getRecipeIngredients()) {
            Ingredient ingredient = recipeIngredient.getIngredient();

            String key = CACHE_KEY + ingredient.getName();
            inMemoryCache.remove(key);
        }

        return convertToDto(updatedRecipe);
    }

    @Transactional
    public RecipeDto patchRecipe(Integer id, RecipeDto partialRecipeDto) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new RecipeException(RECIPE_NOT_FOUND_MESSAGE + id));

        if (partialRecipeDto.getName() != null) {
            recipe.setName(partialRecipeDto.getName());
        }
        if (partialRecipeDto.getPreparationTime() != null) {
            recipe.setPreparationTime(partialRecipeDto.getPreparationTime());
        }

        Recipe updatedRecipe = recipeRepository.save(recipe);

        for (RecipeIngredient recipeIngredient : recipe.getRecipeIngredients()) {
            Ingredient ingredient = recipeIngredient.getIngredient();
            String key = CACHE_KEY + ingredient.getName();
            inMemoryCache.remove(key);
        }
        return convertToDto(updatedRecipe);
    }

    public List<RecipeDto> getRecipesByIngredientFromCacheOrDb(
            String ingredientName, Function<String, List<Recipe>> findRecipesByIngredient) {
        String key = CACHE_KEY + ingredientName;

        if (inMemoryCache.contains(key)) {
            logger.info("Извлечение рецептов для ингредиентов '{}' из кеша", ingredientName);

            Optional<Object> cacheData = inMemoryCache.get(key);

            if (cacheData.isPresent() && cacheData.get() instanceof List<?> cachedList) {
                if (!cachedList.isEmpty() && cachedList.getFirst() instanceof RecipeDto) {
                    @SuppressWarnings("unchecked")
                    List<RecipeDto> cachedRecipes = (List<RecipeDto>) cachedList;
                    return cachedRecipes;
                } else {
                    logger.warn("Данные для ключа '{}' имеют неожиданный тип или пусты.\n", key);
                    return Collections.emptyList();
                }
            } else {
                logger.warn("Промах кеша или неверные данные ключа '{}'\n", key);
                return Collections.emptyList();
            }
        }

        logger.info("Извлечение рецепту по ингредиент '{}' из базы данных", ingredientName);
        List<Recipe> recipes = findRecipesByIngredient.apply(ingredientName);

        List<RecipeDto> recipeDtos = recipes.stream()
                .map(this::convertToDtoWithoutUsersAndComments)
                .toList();

        inMemoryCache.put(key, recipeDtos);

        return recipeDtos;
    }

    public List<CommentDto> getCommentsByRecipeId(Integer id) {
        Recipe recipe = recipeRepository.findById(id).orElseThrow(()
                -> new RecipeException(RECIPE_NOT_FOUND_MESSAGE + id));
        return recipe.getComments().stream().map(this::convertToDto).toList();
    }

    public List<UserDto> getUsersForRecipe(Integer recipeId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RecipeException(RECIPE_NOT_FOUND_MESSAGE + recipeId));

        return recipe.getUsers().stream().map(this::convertToDto).toList();
    }

    public ResponseEntity<String> addUserToRecipe(Integer recipeId, Integer userId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(()
                        -> new RecipeException(RECIPE_NOT_FOUND_MESSAGE + recipeId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND_MESSAGE + userId));

        if (recipe.getUsers().contains(user)) {
            throw new UserAssociatedException("User is already associated with this recipe.");
        }

        recipe.getUsers().add(user);
        user.getSavedRecipes().add(recipe);

        recipeRepository.save(recipe);
        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    public ResponseEntity<Void> removeUserFromRecipe(Integer recipeId, Integer userId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RecipeException(RECIPE_NOT_FOUND_MESSAGE + recipeId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND_MESSAGE + userId));
        recipe.getUsers().remove(user);
        user.getSavedRecipes().remove(recipe);

        recipeRepository.save(recipe);
        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    public ResponseEntity<String> addIngredientToRecipe(Integer recipeId, Integer ingredientId,
                                                        Double quantityInGrams) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RecipeException(RECIPE_NOT_FOUND_MESSAGE + recipeId));
        Ingredient ingredient = ingredientRepository.findById(ingredientId)
                .orElseThrow(() ->
                        new IngredientException(INGREDIENT_NOT_FOUND_MESSAGE + ingredientId));

        boolean ingredientExists = recipe.getRecipeIngredients().stream()
                .anyMatch(ri -> ri.getIngredient().getId().equals(ingredientId));

        if (ingredientExists) {
            throw new IngredientAssociatedException("Ingredient is already "
                    + "associated with this recipe.");
        }

        // Создаем новую связь
        RecipeIngredient recipeIngredient = new RecipeIngredient();
        recipeIngredient.setRecipe(recipe);
        recipeIngredient.setIngredient(ingredient);
        recipeIngredient.setQuantityInGrams(quantityInGrams);

        recipe.getRecipeIngredients().add(recipeIngredient);
        recipeRepository.save(recipe);

        for (RecipeIngredient recipeIngredients : recipe.getRecipeIngredients()) {
            Ingredient recIngredients = recipeIngredients.getIngredient();
            String key = CACHE_KEY + recIngredients.getName();
            inMemoryCache.remove(key);
        }

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    public ResponseEntity<Void> removeIngredientFromRecipe(Integer recipeId, Integer ingredientId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RecipeException(RECIPE_NOT_FOUND_MESSAGE + recipeId));

        RecipeIngredient recipeIngredient = recipe.getRecipeIngredients().stream()
                .filter(ri -> ri.getIngredient().getId().equals(ingredientId))
                .findFirst()
                .orElseThrow(() ->
                        new IngredientException(INGREDIENT_NOT_FOUND_MESSAGE + ingredientId));

        for (RecipeIngredient recipeIngredients : recipe.getRecipeIngredients()) {
            Ingredient recIngredients = recipeIngredients.getIngredient();
            String key = CACHE_KEY + recIngredients.getName();
            inMemoryCache.remove(key);
        }

        recipe.getRecipeIngredients().remove(recipeIngredient);
        recipeRepository.save(recipe);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    public List<RecipeIngredientDto> getIngredientsForRecipe(Integer recipeId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RecipeException(RECIPE_NOT_FOUND_MESSAGE + recipeId));

        return recipe.getRecipeIngredients().stream()
                .map(this::convertToDto)
                .toList();
    }

    private RecipeDto convertToDto(Recipe recipe, boolean includeUsers, boolean includeComments) {
        RecipeDto recipeDto = new RecipeDto();
        recipeDto.setId(recipe.getId());
        recipeDto.setName(recipe.getName());
        recipeDto.setPreparationTime(recipe.getPreparationTime());

        if (recipe.getRecipeIngredients() != null) {
            List<RecipeIngredientDto> recipeIngredientDtos = recipe.getRecipeIngredients().stream()
                    .map(this::convertToDto)
                    .toList();
            recipeDto.setRecipeIngredients(recipeIngredientDtos);
        }

        if (includeUsers && recipe.getUsers() != null) {
            List<UserDto> userDtos = recipe.getUsers().stream()
                    .map(this::convertToDto)
                    .toList();
            recipeDto.setUsers(userDtos);
        }

        if (includeComments && recipe.getComments() != null) {
            List<CommentDto> commentDtos = recipe.getComments().stream()
                    .map(this::convertToDto)
                    .toList();
            recipeDto.setComments(commentDtos);
        }

        return recipeDto;
    }

    private RecipeIngredientDto convertToDto(RecipeIngredient recipeIngredient) {
        RecipeIngredientDto dto = new RecipeIngredientDto();
        dto.setRecipeId(recipeIngredient.getRecipe().getId());
        dto.setIngredientId(recipeIngredient.getIngredient().getId());
        IngredientDto ingredientDto = convertToDto(recipeIngredient.getIngredient());
        dto.setIngredient(ingredientDto);
        dto.setQuantityInGrams(recipeIngredient.getQuantityInGrams());
        return dto;
    }

    private IngredientDto convertToDto(Ingredient ingredient) {
        IngredientDto ingredientDto = new IngredientDto();
        ingredientDto.setId(ingredient.getId());
        ingredientDto.setName(ingredient.getName());
        ingredientDto.setProteins(ingredient.getProteins());
        ingredientDto.setFats(ingredient.getFats());
        ingredientDto.setCarbohydrates(ingredient.getCarbohydrates());
        return ingredientDto;
    }

    private CommentDto convertToDto(Comment comment) {
        CommentDto commentDto = new CommentDto();
        commentDto.setId(comment.getId());
        commentDto.setText(comment.getText());
        commentDto.setUserId(comment.getUserId());
        commentDto.setRecipeId(comment.getRecipeId());
        return commentDto;
    }

    private UserDto convertToDto(User user) {
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setUsername(user.getUsername());
        userDto.setEmail(user.getEmail());
        return userDto;
    }

    private RecipeDto convertToDto(Recipe recipe) {
        return convertToDto(recipe, true, true);
    }

    private RecipeDto convertToDtoWithoutUsersAndComments(Recipe recipe) {
        return convertToDto(recipe, false, false);
    }

    private Recipe convertToEntity(RecipeDto recipeDto) {
        Recipe recipe = new Recipe();
        recipe.setId(recipeDto.getId());
        recipe.setName(recipeDto.getName());
        recipe.setPreparationTime(recipeDto.getPreparationTime());
        return recipe;
    }
}
