package com.gnomeland.foodlab.testservice;

import com.gnomeland.foodlab.cache.InMemoryCache;
import com.gnomeland.foodlab.dto.*;
import com.gnomeland.foodlab.exception.*;
import com.gnomeland.foodlab.model.*;
import com.gnomeland.foodlab.repository.*;
import com.gnomeland.foodlab.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.time.Duration;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private IngredientRepository ingredientRepository;

    @Mock
    private InMemoryCache inMemoryCache;

    @InjectMocks
    private RecipeService recipeService;

    private Recipe recipe;
    private RecipeDto recipeDto;
    private User user;
    private Ingredient ingredient;
    private RecipeIngredient recipeIngredient;

    @BeforeEach
    void setUp() {
        recipe = new Recipe();
        recipe.setId(1);
        recipe.setName("Test Recipe");
        recipe.setPreparationTime(Duration.ofMinutes(30));
        recipe.setUsers(new ArrayList<>()); // Изменено на HashSet
        recipe.setRecipeIngredients(new ArrayList<>());
        recipe.setComments(new ArrayList<>());

        recipeDto = new RecipeDto();
        recipeDto.setId(1);
        recipeDto.setName("Test Recipe");
        recipeDto.setPreparationTime(Duration.ofMinutes(30));

        user = new User();
        user.setId(1);
        user.setUsername("testuser");
        user.setSavedRecipes(new HashSet<>()); // Изменено на HashSet

        ingredient = new Ingredient();
        ingredient.setId(1);
        ingredient.setName("Test Ingredient");
        ingredient.setRecipeIngredients(new ArrayList<>());

        recipeIngredient = new RecipeIngredient();
        recipeIngredient.setRecipe(recipe);
        recipeIngredient.setIngredient(ingredient);
        recipeIngredient.setQuantityInGrams(100.0);
    }
    // Тесты начнутся здесь
    @Test
    void getRecipes_shouldReturnFilteredRecipes_whenNameProvided() {
        // Arrange
        Recipe recipe2 = new Recipe();
        recipe2.setId(2);
        recipe2.setName("Another Recipe");

        when(recipeRepository.findAll()).thenReturn(Arrays.asList(recipe, recipe2));

        // Act
        List<RecipeDto> result = recipeService.getRecipes("Test Recipe");

        // Assert
        assertEquals(1, result.size());
        assertEquals("Test Recipe", result.getFirst().getName());
    }

    @Test
    void getRecipes_shouldReturnAllRecipes_whenNameNotProvided() {
        // Arrange
        when(recipeRepository.findAll()).thenReturn(List.of(recipe)); // Используем List.of() вместо Arrays.asList()

        // Act
        List<RecipeDto> result = recipeService.getRecipes(null);

        // Assert
        assertEquals(1, result.size());
    }

    @Test
    void getRecipes_shouldThrowException_whenNoRecipesFound() {
        // Arrange
        when(recipeRepository.findAll()).thenReturn(Collections.emptyList());

        // Act & Assert
        assertThrows(RecipeException.class, () -> recipeService.getRecipes(null));
    }

    @Test
    void getRecipeById_shouldReturnRecipe_whenExists() {
        // Arrange
        when(recipeRepository.findById(1)).thenReturn(Optional.of(recipe));

        // Act
        RecipeDto result = recipeService.getRecipeById(1);

        // Assert
        assertEquals("Test Recipe", result.getName());
    }

    @Test
    void getRecipeById_shouldThrowException_whenNotFound() {
        // Arrange
        when(recipeRepository.findById(1)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RecipeException.class, () -> recipeService.getRecipeById(1));
    }

    @Test
    void addRecipe_shouldSaveAndReturnRecipe() {
        // Arrange
        when(recipeRepository.save(any(Recipe.class))).thenReturn(recipe);

        // Act
        RecipeDto result = recipeService.addRecipe(recipeDto);

        // Assert
        assertEquals("Test Recipe", result.getName());
        verify(recipeRepository).save(any(Recipe.class));
    }

    @Test
    void deleteRecipeById_shouldDeleteRecipeAndCleanRelations() {
        // Arrange
        // Убедимся, что коллекции правильно инициализированы
        user.getSavedRecipes().add(recipe); // Добавляем рецепт в savedRecipes
        ingredient.getRecipeIngredients().add(recipeIngredient); // Добавляем связь

        recipe.getUsers().add(user);
        recipe.getRecipeIngredients().add(recipeIngredient);

        when(recipeRepository.findById(1)).thenReturn(Optional.of(recipe));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ingredientRepository.save(any(Ingredient.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        recipeService.deleteRecipeById(1);

        // Assert
        verify(recipeRepository).deleteById(1);
        verify(inMemoryCache).remove("recipe_ingredient_Test Ingredient");
        verify(userRepository).save(user);
        verify(ingredientRepository).save(ingredient);

        // Дополнительные проверки состояния
        assertFalse(user.getSavedRecipes().contains(recipe));
        assertFalse(ingredient.getRecipeIngredients().contains(recipeIngredient));
    }

    @Test
    void deleteRecipeById_shouldThrowException_whenRecipeNotFound() {
        // Arrange
        when(recipeRepository.findById(1)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RecipeException.class, () -> recipeService.deleteRecipeById(1));
    }

    @Test
    void updateRecipe_shouldUpdateRecipeAndCleanCache() {
        // Arrange
        RecipeDto updatedDto = new RecipeDto();
        updatedDto.setName("Updated Recipe");
        updatedDto.setPreparationTime(Duration.ofHours(1));

        RecipeIngredientDto ingredientDto = new RecipeIngredientDto();
        ingredientDto.setIngredientId(1);
        ingredientDto.setQuantityInGrams(200.0);
        updatedDto.setRecipeIngredients(List.of(ingredientDto));

        when(recipeRepository.findById(1)).thenReturn(Optional.of(recipe));
        when(ingredientRepository.findById(1)).thenReturn(Optional.of(ingredient));
        when(recipeRepository.save(any(Recipe.class))).thenReturn(recipe);

        // Act
        recipeService.updateRecipe(1, updatedDto); // Убрана неиспользуемая переменная result

        // Assert
        assertEquals("Updated Recipe", recipe.getName());
        assertEquals(1, recipe.getRecipeIngredients().size());
        verify(inMemoryCache).remove("recipe_ingredient_Test Ingredient");
    }

    @Test
    void getRecipesByIngredientFromCacheOrDb_shouldReturnFromCache_whenExists() {
        // Arrange
        List<RecipeDto> cachedRecipes = List.of(recipeDto);
        when(inMemoryCache.contains("recipe_ingredient_Test")).thenReturn(true);
        when(inMemoryCache.get("recipe_ingredient_Test")).thenReturn(Optional.of(cachedRecipes));

        // Act
        List<RecipeDto> result = recipeService.getRecipesByIngredientFromCacheOrDb(
                "Test", name -> Collections.emptyList());

        // Assert
        assertEquals(1, result.size());
        assertEquals("Test Recipe", result.getFirst().getName());
    }

    @Test
    void getRecipesByIngredientFromCacheOrDb_shouldReturnFromDbAndCache_whenNotInCache() {
        // Arrange
        when(inMemoryCache.contains("recipe_ingredient_Test")).thenReturn(false);
        when(recipeRepository.findRecipesByIngredientName("Test")).thenReturn(List.of(recipe));

        // Act
        List<RecipeDto> result = recipeService.getRecipesByIngredientFromCacheOrDb(
                "Test", recipeRepository::findRecipesByIngredientName);

        // Assert
        assertEquals(1, result.size());
        verify(inMemoryCache).put(eq("recipe_ingredient_Test"), anyList());
    }

    @Test
    void addUserToRecipe_shouldAddUserToRecipe() {
        // Arrange
        when(recipeRepository.findById(1)).thenReturn(Optional.of(recipe));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(recipeRepository.save(recipe)).thenReturn(recipe);
        when(userRepository.save(user)).thenReturn(user);

        // Act
        ResponseEntity<String> response = recipeService.addUserToRecipe(1, 1);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(recipe.getUsers().contains(user));
        assertTrue(user.getSavedRecipes().contains(recipe));
    }

    @Test
    void addUserToRecipe_shouldThrowException_whenUserAlreadyAssociated() {
        // Arrange
        recipe.getUsers().add(user);
        when(recipeRepository.findById(1)).thenReturn(Optional.of(recipe));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        // Act & Assert
        assertThrows(UserAssociatedException.class,
                () -> recipeService.addUserToRecipe(1, 1));
    }

    @Test
    void addIngredientToRecipe_shouldAddIngredientToRecipe() {
        // Arrange
        when(recipeRepository.findById(1)).thenReturn(Optional.of(recipe));
        when(ingredientRepository.findById(1)).thenReturn(Optional.of(ingredient));
        when(recipeRepository.save(recipe)).thenReturn(recipe);

        // Act
        ResponseEntity<String> response = recipeService.addIngredientToRecipe(1, 1, 100.0);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(1, recipe.getRecipeIngredients().size());
        verify(inMemoryCache).remove("recipe_ingredient_Test Ingredient");
    }

    @Test
    void addIngredientToRecipe_shouldThrowException_whenIngredientAlreadyAssociated() {
        // Arrange
        recipe.getRecipeIngredients().add(recipeIngredient);
        when(recipeRepository.findById(1)).thenReturn(Optional.of(recipe));
        when(ingredientRepository.findById(1)).thenReturn(Optional.of(ingredient));

        // Act & Assert
        assertThrows(IngredientAssociatedException.class,
                () -> recipeService.addIngredientToRecipe(1, 1, 100.0));
    }

    @Test
    void removeIngredientFromRecipe_shouldRemoveIngredientAndCleanCache() {
        // Arrange
        recipe.getRecipeIngredients().add(recipeIngredient);

        when(recipeRepository.findById(1)).thenReturn(Optional.of(recipe));
        when(recipeRepository.save(recipe)).thenReturn(recipe);

        // Act
        ResponseEntity<Void> response = recipeService.removeIngredientFromRecipe(1, 1);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertTrue(recipe.getRecipeIngredients().isEmpty());
        verify(inMemoryCache).remove("recipe_ingredient_Test Ingredient");
    }

    @Test
    void removeIngredientFromRecipe_shouldThrowException_whenIngredientNotFound() {
        // Arrange
        when(recipeRepository.findById(1)).thenReturn(Optional.of(recipe));

        // Act & Assert
        assertThrows(IngredientException.class,
                () -> recipeService.removeIngredientFromRecipe(1, 1));
    }

    @Test
    void patchRecipe_shouldUpdateOnlyProvidedFields() {
        // Arrange
        RecipeDto partialDto = new RecipeDto();
        partialDto.setName("Patched Recipe");

        // Добавляем ингредиент, чтобы проверить очистку кэша
        recipe.getRecipeIngredients().add(recipeIngredient);

        when(recipeRepository.findById(1)).thenReturn(Optional.of(recipe));
        when(recipeRepository.save(recipe)).thenReturn(recipe);

        // Act
        RecipeDto result = recipeService.patchRecipe(1, partialDto);

        // Assert
        assertEquals("Patched Recipe", result.getName());
        assertEquals(Duration.ofMinutes(30), result.getPreparationTime());
        verify(inMemoryCache).remove("recipe_ingredient_Test Ingredient");
    }

    @Test
    void patchRecipe_shouldNotUpdateFields_whenNull() {
        // Arrange
        RecipeDto partialDto = new RecipeDto();

        when(recipeRepository.findById(1)).thenReturn(Optional.of(recipe));
        when(recipeRepository.save(recipe)).thenReturn(recipe);

        // Act
        RecipeDto result = recipeService.patchRecipe(1, partialDto);

        // Assert
        assertEquals("Test Recipe", result.getName());
    }

    @Test
    void deleteRecipeById_shouldHandleEmptyRelations() {
        // Arrange
        recipe.setUsers(new ArrayList<>());
        recipe.setRecipeIngredients(new ArrayList<>());

        when(recipeRepository.findById(1)).thenReturn(Optional.of(recipe));

        // Act
        recipeService.deleteRecipeById(1);

        // Assert
        verify(recipeRepository).deleteById(1);
        verifyNoInteractions(userRepository, ingredientRepository);
    }

    @Test
    void removeUserFromRecipe_shouldRemoveUserFromRecipe() {
        // Arrange
        recipe.getUsers().add(user);
        user.getSavedRecipes().add(recipe);

        when(recipeRepository.findById(1)).thenReturn(Optional.of(recipe));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(recipeRepository.save(recipe)).thenReturn(recipe);
        when(userRepository.save(user)).thenReturn(user);

        // Act
        ResponseEntity<Void> response = recipeService.removeUserFromRecipe(1, 1);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertFalse(recipe.getUsers().contains(user));
        assertFalse(user.getSavedRecipes().contains(recipe));
    }

    @Test
    void removeUserFromRecipe_shouldNotThrow_whenUserNotAssociated() {
        // Arrange
        when(recipeRepository.findById(1)).thenReturn(Optional.of(recipe));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(recipeRepository.save(recipe)).thenReturn(recipe);
        when(userRepository.save(user)).thenReturn(user);

        // Act
        ResponseEntity<Void> response = recipeService.removeUserFromRecipe(1, 1);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void addRecipesBulk_shouldAddMultipleRecipes() {
        // Arrange
        RecipeDto recipeDto2 = new RecipeDto();
        recipeDto2.setName("Recipe 2");

        when(recipeRepository.save(any(Recipe.class)))
                .thenReturn(recipe)
                .thenAnswer(inv -> {
                    Recipe r = inv.getArgument(0);
                    r.setId(2);
                    return r;
                });

        // Act
        List<RecipeDto> result = recipeService.addRecipesBulk(List.of(recipeDto, recipeDto2));

        // Assert
        assertEquals(2, result.size());
        assertEquals("Test Recipe", result.get(0).getName());
        assertEquals("Recipe 2", result.get(1).getName());
    }

    @Test
    void getCommentsByRecipeId_shouldReturnComments() {
        // Arrange
        Comment comment = new Comment();
        comment.setText("Test comment");
        recipe.setComments(List.of(comment));

        when(recipeRepository.findById(1)).thenReturn(Optional.of(recipe));

        // Act
        List<CommentDto> result = recipeService.getCommentsByRecipeId(1);

        // Assert
        assertEquals(1, result.size());
        assertEquals("Test comment", result.getFirst().getText());
    }

    @Test
    void getCommentsByRecipeId_shouldThrowException_whenRecipeNotFound() {
        // Arrange
        when(recipeRepository.findById(1)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RecipeException.class,
                () -> recipeService.getCommentsByRecipeId(1));
    }

    @Test
    void getUsersForRecipe_shouldReturnUsers() {
        // Arrange
        recipe.getUsers().add(user);

        when(recipeRepository.findById(1)).thenReturn(Optional.of(recipe));

        // Act
        List<UserDto> result = recipeService.getUsersForRecipe(1);

        // Assert
        assertEquals(1, result.size());
        assertEquals("testuser", result.getFirst().getUsername());
    }

    @Test
    void getIngredientsForRecipe_shouldReturnIngredients() {
        // Arrange
        recipe.getRecipeIngredients().add(recipeIngredient);

        when(recipeRepository.findById(1)).thenReturn(Optional.of(recipe));

        // Act
        List<RecipeIngredientDto> result = recipeService.getIngredientsForRecipe(1);

        // Assert
        assertEquals(1, result.size());
        assertEquals(100.0, result.getFirst().getQuantityInGrams());
    }

    @Test
    void getRecipesByIngredientFromCacheOrDb_shouldHandleEmptyCacheData() {
        // Arrange
        when(inMemoryCache.contains("recipe_ingredient_Test")).thenReturn(true);
        when(inMemoryCache.get("recipe_ingredient_Test")).thenReturn(Optional.empty());

        // Act
        List<RecipeDto> result = recipeService.getRecipesByIngredientFromCacheOrDb(
                "Test", name -> Collections.emptyList());

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void getRecipesByIngredientFromCacheOrDb_shouldHandleWrongCacheType() {
        // Arrange
        when(inMemoryCache.contains("recipe_ingredient_Test")).thenReturn(true);
        when(inMemoryCache.get("recipe_ingredient_Test")).thenReturn(Optional.of("wrong type"));

        // Act
        List<RecipeDto> result = recipeService.getRecipesByIngredientFromCacheOrDb(
                "Test", name -> Collections.emptyList());

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void updateRecipe_shouldHandleNewIngredients() {
        // Arrange
        RecipeIngredientDto ingredientDto = new RecipeIngredientDto();
        ingredientDto.setIngredientId(1);
        ingredientDto.setQuantityInGrams(200.0);

        IngredientDto ingredientDetails = new IngredientDto();
        ingredientDetails.setId(1);
        ingredientDetails.setName("Test Ingredient");
        ingredientDto.setIngredient(ingredientDetails);

        RecipeDto updatedDto = new RecipeDto();
        updatedDto.setRecipeIngredients(List.of(ingredientDto));

        when(recipeRepository.findById(1)).thenReturn(Optional.of(recipe));
        when(ingredientRepository.findById(1)).thenReturn(Optional.of(ingredient));
        when(recipeRepository.save(recipe)).thenReturn(recipe);

        // Act
        recipeService.updateRecipe(1, updatedDto);

        // Assert
        assertEquals(1, recipe.getRecipeIngredients().size());
        assertEquals(200.0, recipe.getRecipeIngredients().getFirst().getQuantityInGrams());
    }

    @Test
    void deleteRecipeById_shouldHandleMultipleIngredients() {
        // Arrange
        Ingredient ingredient2 = new Ingredient();
        ingredient2.setId(2);
        ingredient2.setName("Ingredient 2");
        ingredient2.setRecipeIngredients(new ArrayList<>());

        RecipeIngredient ri2 = new RecipeIngredient();
        ri2.setIngredient(ingredient2);
        ri2.setRecipe(recipe);

        ingredient.setRecipeIngredients(new ArrayList<>());
        recipe.getRecipeIngredients().add(recipeIngredient);
        recipe.getRecipeIngredients().add(ri2);

        when(recipeRepository.findById(1)).thenReturn(Optional.of(recipe));
        // Убираем ненужные стабы для save, так как они не используются в этом сценарии

        // Act
        recipeService.deleteRecipeById(1);

        // Assert
        verify(inMemoryCache).remove("recipe_ingredient_Test Ingredient");
        verify(inMemoryCache).remove("recipe_ingredient_Ingredient 2");
        verify(recipeRepository).deleteById(1);
    }

    @Test
    void patchRecipe_shouldNotClearCache_whenNoIngredientsChanged() {
        // Arrange
        RecipeDto partialDto = new RecipeDto();
        partialDto.setPreparationTime(Duration.ofHours(1));

        when(recipeRepository.findById(1)).thenReturn(Optional.of(recipe));
        when(recipeRepository.save(recipe)).thenReturn(recipe);

        // Act
        RecipeDto result = recipeService.patchRecipe(1, partialDto);

        // Assert
        assertEquals(Duration.ofHours(1), result.getPreparationTime());
        verify(inMemoryCache, never()).remove(anyString());
    }
}