package com.gnomeland.foodlab.service;

import com.gnomeland.foodlab.cache.RecipeCache;
import com.gnomeland.foodlab.dto.CommentDto;
import com.gnomeland.foodlab.dto.IngredientDto;
import com.gnomeland.foodlab.dto.RecipeDto;
import com.gnomeland.foodlab.dto.RecipeIngredientDto;
import com.gnomeland.foodlab.dto.UserDto;
import com.gnomeland.foodlab.exception.IngredientNotFoundException;
import com.gnomeland.foodlab.exception.RecipeNotFoundException;
import com.gnomeland.foodlab.exception.UserNotFoundException;
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
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;


@Service
public class RecipeService {
    private static final String CACHE_KEY_RECIPES_ALL = "recipes:all";
    private static final String CACHE_KEY_RECIPE_PREFIX = "recipe:";
    private static final String CACHE_KEY_INGREDIENTS_PREFIX = "ingredients:";
    private static final String CACHE_KEY_RECIPES_BY_INGREDIENT_PREFIX = "recipesByIngredient:";
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final IngredientRepository ingredientRepository;
    private final RecipeCache recipeCache;
    private static final Logger logger = LoggerFactory.getLogger(RecipeService.class);

    @Autowired
    public RecipeService(RecipeRepository recipeRepository, UserRepository userRepository,
                         IngredientRepository ingredientRepository, RecipeCache recipeCache) {
        this.recipeRepository = recipeRepository;
        this.userRepository = userRepository;
        this.ingredientRepository = ingredientRepository;
        this.recipeCache = recipeCache;
    }

    public List<RecipeDto> getRecipes(String name) {
        return recipeRepository.findAll().stream()
                .filter(recipe -> name == null || recipe.getName().equalsIgnoreCase(name))
                .map(this::convertToDto)
                .toList();
    }

    public RecipeDto getRecipeById(Integer id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new RecipeNotFoundException(id));
        return convertToDto(recipe);
    }

    @Transactional
    public RecipeDto addRecipe(RecipeDto recipeDto) {
        Recipe recipe = convertToEntity(recipeDto);
        RecipeDto result = convertToDto(recipeRepository.save(recipe));

        // Очищаем кэш для общего списка рецептов
        recipeCache.remove(CACHE_KEY_RECIPES_ALL);
        return result;
    }

    @Transactional
    public void deleteRecipeById(Integer id) {
        if (recipeRepository.existsById(id)) {
            recipeRepository.deleteById(id);

            // Очищаем кэш для удаленного рецепта и общего списка рецептов
            recipeCache.remove(CACHE_KEY_RECIPE_PREFIX + id);
            recipeCache.remove(CACHE_KEY_RECIPES_ALL);
            recipeCache.removeByPrefix(CACHE_KEY_RECIPES_BY_INGREDIENT_PREFIX);
        } else {
            throw new RecipeNotFoundException(id);
        }
    }

    @Transactional
    public RecipeDto updateRecipe(Integer id, RecipeDto updatedRecipeDto) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new RecipeNotFoundException(id));

        recipe.setName(updatedRecipeDto.getName());
        recipe.setPreparationTime(updatedRecipeDto.getPreparationTime());

        // Обновляем ингредиенты и другие поля
        List<RecipeIngredient> updatedIngredients = updatedRecipeDto.getRecipeIngredients().stream()
                .map(dto -> {
                    RecipeIngredient recipeIngredient = new RecipeIngredient();
                    recipeIngredient.setRecipe(recipe);
                    recipeIngredient.setIngredient(ingredientRepository
                            .findById(dto.getIngredientId())
                            .orElseThrow(() ->
                                    new IllegalArgumentException("Ingredient not found")));
                    recipeIngredient.setQuantityInGrams(dto.getQuantityInGrams());
                    return recipeIngredient;
                })
                .toList();

        recipe.getRecipeIngredients().clear();
        recipe.getRecipeIngredients().addAll(updatedIngredients);

        RecipeDto result = convertToDto(recipeRepository.save(recipe));

        recipeCache.remove(CACHE_KEY_RECIPE_PREFIX + id);
        recipeCache.remove(CACHE_KEY_RECIPES_ALL);
        recipeCache.removeByPrefix(CACHE_KEY_RECIPES_BY_INGREDIENT_PREFIX);

        return result;
    }

    @Transactional
    public RecipeDto patchRecipe(Integer id, RecipeDto partialRecipeDto) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new RecipeNotFoundException(id));

        if (partialRecipeDto.getName() != null) {
            recipe.setName(partialRecipeDto.getName());
        }
        if (partialRecipeDto.getPreparationTime() != null) {
            recipe.setPreparationTime(partialRecipeDto.getPreparationTime());
        }

        RecipeDto result = convertToDto(recipeRepository.save(recipe));

        // Очищаем кэш для обновленного рецепта и общего списка рецептов
        recipeCache.remove(CACHE_KEY_RECIPE_PREFIX + id);
        recipeCache.remove(CACHE_KEY_RECIPES_ALL);
        recipeCache.removeByPrefix(CACHE_KEY_RECIPES_BY_INGREDIENT_PREFIX);

        return result;
    }

    public List<RecipeDto> getRecipesByIngredient(String ingredientName) {
        // Проверка на null или пустое имя ингредиента
        if (ingredientName == null || ingredientName.isEmpty()) {
            logger.warn("Имя ингредиента не указано. Возвращаем пустой список.");
            return Collections.emptyList();
        }

        // Формируем ключ для кэша
        String cacheKey = CACHE_KEY_RECIPES_BY_INGREDIENT_PREFIX + ingredientName;

        // Проверяем, есть ли данные в кэше
        synchronized (recipeCache) {
            Optional<List<RecipeDto>> cachedRecipes = recipeCache.get(cacheKey);
            if (cachedRecipes.isPresent()) {
                logger.info("Попадание в кэш для ключа: {}", cacheKey);
                return cachedRecipes.get();
            } else {
                logger.info("Промах кэша для ключа: {}", cacheKey);
            }
        }

        // Если данных в кэше нет, выполняем запрос к БД
        List<RecipeDto> recipes = recipeRepository.findRecipesByIngredientName(ingredientName)
                .stream()
                .map(this::convertToDtoWithoutUsersAndComments)
                .toList();

        // Логируем пустой результат, если рецепты не найдены
        if (recipes.isEmpty()) {
            logger.info("Рецепты по ингредиенту '{}' не найдены. Сохраняем пустой список в кэше.", ingredientName);
        }

        // Сохраняем результат в кэше
        synchronized (recipeCache) {
            recipeCache.put(cacheKey, recipes);
        }

        return recipes;
    }

    public List<RecipeDto> getRecipesByIngredientNative(String ingredientName) {
        String searchParam = "%" + ingredientName + "%";
        List<Recipe> recipes = recipeRepository.findRecipesByIngredientNameNative(searchParam);
        return recipes.stream()
                .map(this::convertToDtoWithoutUsersAndComments)
                .toList();
    }

    public List<CommentDto> getCommentsByRecipeId(Integer id) {
        Recipe recipe = recipeRepository.findById(id).orElseThrow(()
                -> new RecipeNotFoundException(id));
        return recipe.getComments().stream().map(this::convertToDto).toList();
    }

    public List<UserDto> getUsersForRecipe(Integer recipeId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RecipeNotFoundException(recipeId));

        return recipe.getUsers().stream().map(this::convertToDto).toList();
    }

    public ResponseEntity<String> addUserToRecipe(Integer recipeId, Integer userId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(()
                        -> new RecipeNotFoundException(recipeId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (recipe.getUsers().contains(user)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("User is already associated with this recipe.");
        }

        recipe.getUsers().add(user);
        user.getSavedRecipes().add(recipe);

        recipeRepository.save(recipe);
        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    public ResponseEntity<Void> removeUserFromRecipe(Integer recipeId, Integer userId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RecipeNotFoundException(recipeId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RecipeNotFoundException(userId));
        recipe.getUsers().remove(user);
        user.getSavedRecipes().remove(recipe);

        recipeRepository.save(recipe);
        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    public ResponseEntity<String> addIngredientToRecipe(Integer recipeId, Integer ingredientId,
                                                        Double quantityInGrams) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RecipeNotFoundException(recipeId));
        Ingredient ingredient = ingredientRepository.findById(ingredientId)
                .orElseThrow(() -> new IngredientNotFoundException(ingredientId));

        boolean ingredientExists = recipe.getRecipeIngredients().stream()
                .anyMatch(ri -> ri.getIngredient().getId().equals(ingredientId));

        if (ingredientExists) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Ingredient is already associated with this recipe.");
        }

        // Создаем новую связь
        RecipeIngredient recipeIngredient = new RecipeIngredient();
        recipeIngredient.setRecipe(recipe);
        recipeIngredient.setIngredient(ingredient);
        recipeIngredient.setQuantityInGrams(quantityInGrams);

        recipe.getRecipeIngredients().add(recipeIngredient);
        recipeRepository.save(recipe);

        recipeCache.remove(CACHE_KEY_INGREDIENTS_PREFIX + ingredientId);
        recipeCache.remove(CACHE_KEY_RECIPE_PREFIX + recipeId);
        recipeCache.removeByPrefix(CACHE_KEY_RECIPES_BY_INGREDIENT_PREFIX);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    public ResponseEntity<Void> removeIngredientFromRecipe(Integer recipeId, Integer ingredientId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RecipeNotFoundException(recipeId));

        RecipeIngredient recipeIngredient = recipe.getRecipeIngredients().stream()
                .filter(ri -> ri.getIngredient().getId().equals(ingredientId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Ingredient not found in recipe"));

        recipe.getRecipeIngredients().remove(recipeIngredient);
        recipeRepository.save(recipe);

        // Очищаем кэш для ингредиента
        String ingredientCacheKey = CACHE_KEY_INGREDIENTS_PREFIX + ingredientId;
        recipeCache.remove(ingredientCacheKey);
        logger.info("Очищен кэш для ингредиента: {}", ingredientCacheKey);

        // Очищаем кэш для рецепта
        String recipeCacheKey = CACHE_KEY_RECIPE_PREFIX + recipeId;
        recipeCache.remove(recipeCacheKey);
        logger.info("Очищен кэш для рецепта: {}", recipeCacheKey);

        // Очищаем кэш для рецептов по ингредиентам
        recipeCache.removeByPrefix(CACHE_KEY_RECIPES_BY_INGREDIENT_PREFIX);
        logger.info("Очищен кэш для рецептов по ингредиентам с префиксом: {}",
                CACHE_KEY_RECIPES_BY_INGREDIENT_PREFIX);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    public List<RecipeIngredientDto> getIngredientsForRecipe(Integer recipeId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RecipeNotFoundException(recipeId));

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
        dto.setId(recipeIngredient.getId());
        dto.setRecipeId(recipeIngredient.getRecipe().getId());

        // Устанавливаем ID ингредиента
        dto.setIngredientId(recipeIngredient.getIngredient().getId());

        // Если нужно включить полную информацию об ингредиенте
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
