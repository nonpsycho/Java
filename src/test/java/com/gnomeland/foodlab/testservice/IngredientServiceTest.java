package com.gnomeland.foodlab.testservice;

import com.gnomeland.foodlab.cache.InMemoryCache;
import com.gnomeland.foodlab.dto.IngredientDto;
import com.gnomeland.foodlab.dto.RecipeIngredientDto;
import com.gnomeland.foodlab.exception.IngredientException;
import com.gnomeland.foodlab.model.Ingredient;
import com.gnomeland.foodlab.model.Recipe;
import com.gnomeland.foodlab.model.RecipeIngredient;
import com.gnomeland.foodlab.repository.IngredientRepository;
import com.gnomeland.foodlab.repository.RecipeIngredientRepository;
import com.gnomeland.foodlab.service.IngredientService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngredientServiceTest {
    private static final String CACHE_KEY_RECIPE_INGREDIENT_PREFIX = "recipe_ingredient_";
    @Mock
    private IngredientRepository ingredientRepository;

    @Mock
    private RecipeIngredientRepository recipeIngredientRepository;

    @Mock
    private InMemoryCache inMemoryCache;

    @InjectMocks
    private IngredientService ingredientService;

    private Ingredient createTestIngredient() {
        Ingredient ingredient = new Ingredient();
        ingredient.setId(1);
        ingredient.setName("Test Ingredient");
        ingredient.setProteins(10.0);
        ingredient.setFats(5.0);
        ingredient.setCarbohydrates(20.0);
        return ingredient;
    }

    private IngredientDto createTestIngredientDto() {
        IngredientDto dto = new IngredientDto();
        dto.setId(1);
        dto.setName("Test Ingredient");
        dto.setProteins(10.0);
        dto.setFats(5.0);
        dto.setCarbohydrates(20.0);
        return dto;
    }

    private RecipeIngredient createTestRecipeIngredient() {
        RecipeIngredient ri = new RecipeIngredient();
        ri.setQuantityInGrams(100.0);

        Recipe recipe = new Recipe();
        recipe.setId(1);
        ri.setRecipe(recipe);

        Ingredient ingredient = createTestIngredient();
        ri.setIngredient(ingredient);

        return ri;
    }

    @Test
    void getIngredients_WithNameFilter() {
        // Arrange
        Ingredient ingredient = createTestIngredient();
        when(ingredientRepository.findByNameIgnoreCase("Test")).thenReturn(List.of(ingredient));

        // Act
        List<IngredientDto> result = ingredientService.getIngredients("Test");

        // Assert
        assertEquals(1, result.size());
        assertEquals("Test Ingredient", result.getFirst().getName());
    }

    @Test
    void getIngredients_WithoutFilter() {
        // Arrange
        Ingredient ingredient = createTestIngredient();
        when(ingredientRepository.findAll()).thenReturn(List.of(ingredient));

        // Act
        List<IngredientDto> result = ingredientService.getIngredients(null);

        // Assert
        assertEquals(1, result.size());
    }

    @Test
    void getIngredients_EmptyResult() {
        // Arrange
        when(ingredientRepository.findByNameIgnoreCase("Unknown")).thenReturn(Collections.emptyList());

        // Act & Assert
        assertThrows(IngredientException.class, () -> ingredientService.getIngredients("Unknown"));
    }

    @Test
    void getIngredientById_Success() {
        // Arrange
        Ingredient ingredient = createTestIngredient();
        when(ingredientRepository.findById(1)).thenReturn(Optional.of(ingredient));

        // Act
        IngredientDto result = ingredientService.getIngredientById(1);

        // Assert
        assertEquals("Test Ingredient", result.getName());
    }

    @Test
    void getIngredientById_NotFound() {
        // Arrange
        when(ingredientRepository.findById(1)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IngredientException.class, () -> ingredientService.getIngredientById(1));
    }

    @Test
    void addIngredient_Success() {
        // Arrange
        IngredientDto dto = createTestIngredientDto();
        when(ingredientRepository.findByNameIgnoreCase("Test Ingredient")).thenReturn(Collections.emptyList());
        when(ingredientRepository.save(any(Ingredient.class))).thenReturn(createTestIngredient());

        // Act
        IngredientDto result = ingredientService.addIngredient(dto);

        // Assert
        assertNotNull(result);
        verify(ingredientRepository).save(any(Ingredient.class));
    }

    @Test
    void addIngredient_AlreadyExists() {
        // Arrange
        IngredientDto dto = createTestIngredientDto();
        when(ingredientRepository.findByNameIgnoreCase("Test Ingredient")).thenReturn(List.of(createTestIngredient()));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> ingredientService.addIngredient(dto));
    }

    @Test
    void updateIngredient_Success() {
        // Arrange
        Ingredient existing = createTestIngredient();
        IngredientDto updateDto = createTestIngredientDto();
        updateDto.setName("Updated Name");

        when(ingredientRepository.findById(1)).thenReturn(Optional.of(existing));
        when(ingredientRepository.save(any(Ingredient.class))).thenReturn(existing);

        // Act
        IngredientDto result = ingredientService.updateIngredient(1, updateDto);

        // Assert
        assertEquals("Updated Name", result.getName());
        verify(inMemoryCache, times(2)).remove(anyString());
    }

    @Test
    void deleteIngredientById_Success() {
        // Arrange
        when(recipeIngredientRepository.findByIngredientId(1)).thenReturn(Collections.emptyList());
        when(ingredientRepository.existsById(1)).thenReturn(true);

        // Act
        ResponseEntity<String> response = ingredientService.deleteIngredientById(1);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(recipeIngredientRepository).findByIngredientId(1);
        verify(inMemoryCache).removeAll();
        verify(ingredientRepository).deleteById(1);
    }

    @Test
    void deleteIngredientById_WithRecipes() {
        // Arrange
        RecipeIngredient ri = createTestRecipeIngredient();
        when(ingredientRepository.existsById(1)).thenReturn(true); // Фиксируем существование ингредиента
        when(recipeIngredientRepository.findByIngredientId(1)).thenReturn(List.of(ri));

        // Act & Assert
        IngredientException exception = assertThrows(
                IngredientException.class,
                () -> ingredientService.deleteIngredientById(1)
        );

        assertEquals("Cannot delete ingredient with existing recipes", exception.getMessage());
        verify(ingredientRepository, never()).deleteById(any()); // Убеждаемся, что удаление не вызвано
    }

    @Test
    void patchIngredient_PartialUpdate() {
        // Arrange
        Ingredient existing = createTestIngredient();
        IngredientDto partialDto = new IngredientDto();
        partialDto.setProteins(15.0);

        when(ingredientRepository.findById(1)).thenReturn(Optional.of(existing));
        when(ingredientRepository.save(any(Ingredient.class))).thenReturn(existing);

        // Act
        IngredientDto result = ingredientService.patchIngredient(1, partialDto);

        // Assert
        assertEquals(15.0, result.getProteins());
        assertEquals(5.0, result.getFats());
    }

    @Test
    void getRecipesByIngredientId_Success() {
        // Arrange
        Ingredient ingredient = createTestIngredient();
        RecipeIngredient ri = createTestRecipeIngredient();
        ingredient.setRecipeIngredients(List.of(ri));

        when(ingredientRepository.findById(1)).thenReturn(Optional.of(ingredient));

        // Act
        List<RecipeIngredientDto> result = ingredientService.getRecipesByIngredientId(1);

        // Assert
        assertEquals(1, result.size());
        assertEquals(1, result.getFirst().getRecipeId());
    }

    @Test
    void updateIngredient_ShouldInvalidateOldAndNewCacheKeys() {
        // Arrange
        Ingredient existing = createTestIngredient();
        IngredientDto updateDto = createTestIngredientDto();
        updateDto.setName("New Name");

        when(ingredientRepository.findById(1)).thenReturn(Optional.of(existing));
        when(ingredientRepository.save(any())).thenReturn(existing);

        // Act
        ingredientService.updateIngredient(1, updateDto);

        // Assert
        verify(inMemoryCache).remove(CACHE_KEY_RECIPE_INGREDIENT_PREFIX + "Test Ingredient");
        verify(inMemoryCache).remove(CACHE_KEY_RECIPE_INGREDIENT_PREFIX + "New Name");
    }

    @Test
    void patchIngredient_ShouldUpdateNameAndInvalidateCache() {
        // Arrange
        Ingredient existing = createTestIngredient();
        IngredientDto partialDto = new IngredientDto();
        partialDto.setName("Patched Name");

        when(ingredientRepository.findById(1)).thenReturn(Optional.of(existing));
        when(ingredientRepository.save(any())).thenReturn(existing);

        // Act
        ingredientService.patchIngredient(1, partialDto);

        // Assert
        verify(inMemoryCache).remove(CACHE_KEY_RECIPE_INGREDIENT_PREFIX + "Test Ingredient");
        verify(inMemoryCache).remove(CACHE_KEY_RECIPE_INGREDIENT_PREFIX + "Patched Name");
    }

    @Test
    void getRecipesByIngredientId_ShouldMapAllFieldsCorrectly() {
        // Arrange
        Ingredient ingredient = createTestIngredient();
        RecipeIngredient ri = createTestRecipeIngredient();
        ingredient.setRecipeIngredients(List.of(ri));

        when(ingredientRepository.findById(1)).thenReturn(Optional.of(ingredient));

        // Act
        List<RecipeIngredientDto> result = ingredientService.getRecipesByIngredientId(1);

        // Assert
        assertEquals(1, result.size());
        RecipeIngredientDto dto = result.getFirst();

        assertEquals(1, dto.getRecipeId());
        assertEquals(1, dto.getIngredientId());
        assertEquals(100.0, dto.getQuantityInGrams());
    }

    @Test
    void patchIngredient_ShouldUpdateOnlyProteins() {
        // Arrange
        Ingredient existing = createTestIngredient();
        IngredientDto partialDto = new IngredientDto();
        partialDto.setProteins(99.0);

        when(ingredientRepository.findById(1)).thenReturn(Optional.of(existing));
        when(ingredientRepository.save(any())).thenReturn(existing);

        // Act
        IngredientDto result = ingredientService.patchIngredient(1, partialDto);

        // Assert
        assertEquals(99.0, result.getProteins());
        assertEquals("Test Ingredient", result.getName()); // Имя осталось прежним
    }

    @Test
    void getIngredientById_ShouldHandleNullRecipeIngredients() {
        // Arrange
        Ingredient ingredient = createTestIngredient();
        ingredient.setRecipeIngredients(null); // Явно задаем null
        when(ingredientRepository.findById(1)).thenReturn(Optional.of(ingredient));

        // Act
        IngredientDto dto = ingredientService.getIngredientById(1);

        // Assert
        assertNull(dto.getRecipeIngredients());
    }

    @Test
    void patchIngredient_ShouldUpdateOnlyFats() {
        // Arrange
        Ingredient existing = createTestIngredient();
        IngredientDto partialDto = new IngredientDto();
        partialDto.setFats(15.0);

        when(ingredientRepository.findById(1)).thenReturn(Optional.of(existing));
        when(ingredientRepository.save(any())).thenReturn(existing);

        // Act
        IngredientDto result = ingredientService.patchIngredient(1, partialDto);

        // Assert
        assertEquals(15.0, result.getFats());
        assertEquals(10.0, result.getProteins()); // Белки не изменились
        assertEquals(20.0, result.getCarbohydrates()); // Углеводы не изменились
        assertEquals("Test Ingredient", result.getName()); // Имя не изменилось
    }

    @Test
    void patchIngredient_ShouldUpdateOnlyCarbohydrates() {
        // Arrange
        Ingredient existing = createTestIngredient();
        IngredientDto partialDto = new IngredientDto();
        partialDto.setCarbohydrates(30.0);

        when(ingredientRepository.findById(1)).thenReturn(Optional.of(existing));
        when(ingredientRepository.save(any())).thenReturn(existing);

        // Act
        IngredientDto result = ingredientService.patchIngredient(1, partialDto);

        // Assert
        assertEquals(30.0, result.getCarbohydrates());
        assertEquals(10.0, result.getProteins()); // Белки не изменились
        assertEquals(5.0, result.getFats()); // Жиры не изменились
    }

    @Test
    void patchIngredient_ShouldUpdateMultipleFields() {
        // Arrange
        Ingredient existing = createTestIngredient();
        IngredientDto partialDto = new IngredientDto();
        partialDto.setFats(8.0);
        partialDto.setCarbohydrates(25.0);

        when(ingredientRepository.findById(1)).thenReturn(Optional.of(existing));
        when(ingredientRepository.save(any())).thenReturn(existing);

        // Act
        IngredientDto result = ingredientService.patchIngredient(1, partialDto);

        // Assert
        assertEquals(8.0, result.getFats());
        assertEquals(25.0, result.getCarbohydrates());
        assertEquals(10.0, result.getProteins()); // Белки не изменились
    }

    @Test
    void patchIngredient_ShouldUpdateCacheWhenNameChanged() {
        // Arrange
        Ingredient existing = createTestIngredient();
        IngredientDto partialDto = new IngredientDto();
        partialDto.setName("New Name");

        when(ingredientRepository.findById(1)).thenReturn(Optional.of(existing));
        when(ingredientRepository.save(any())).thenReturn(existing);

        // Act
        ingredientService.patchIngredient(1, partialDto);

        // Assert
        verify(inMemoryCache).remove(CACHE_KEY_RECIPE_INGREDIENT_PREFIX + "Test Ingredient");
        verify(inMemoryCache).remove(CACHE_KEY_RECIPE_INGREDIENT_PREFIX + "New Name");
    }

    @Test
    void patchIngredient_ShouldThrowExceptionWhenIngredientNotFound() {
        // Arrange
        when(ingredientRepository.findById(1)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IngredientException.class, this::callPatchIngredient);
    }

    private void callPatchIngredient() {
        ingredientService.patchIngredient(1, new IngredientDto());
    }

    @Test
    void patchIngredient_ShouldHandleNullDto() {
        // Arrange
        Ingredient existing = createTestIngredient();
        when(ingredientRepository.findById(1)).thenReturn(Optional.of(existing));

        // Act & Assert
        assertThrows(NullPointerException.class, () ->
                ingredientService.patchIngredient(1, null));
    }


    @Test
    void deleteIngredientById_ShouldCheckExistenceBeforeCheckingRecipes() {
        // Arrange
        when(ingredientRepository.existsById(1)).thenReturn(false);

        // Act & Assert
        assertThrows(IngredientException.class, () -> ingredientService.deleteIngredientById(1));

        // Verify recipe check wasn't called if ingredient doesn't exist
        verify(recipeIngredientRepository, never()).findByIngredientId(any());
    }

    @Test
    void deleteIngredientById_ShouldNotProceedIfIngredientDoesNotExist() {
        // Arrange
        when(ingredientRepository.existsById(1)).thenReturn(false);

        // Act & Assert
        assertThrows(IngredientException.class, () -> ingredientService.deleteIngredientById(1));

        // Verify no further operations were attempted
        verify(recipeIngredientRepository, never()).findByIngredientId(any());
        verify(inMemoryCache, never()).removeAll();
        verify(ingredientRepository, never()).deleteById(any());
    }

    @Test
    void deleteIngredientById_ShouldCheckExistenceFirst() {
        // Arrange
        when(ingredientRepository.existsById(1)).thenReturn(false);

        // Act & Assert
        assertThrows(IngredientException.class, () -> ingredientService.deleteIngredientById(1));

        // Verify the order of operations - existence check comes first
        InOrder inOrder = inOrder(ingredientRepository, recipeIngredientRepository);
        inOrder.verify(ingredientRepository).existsById(1);
        inOrder.verify(recipeIngredientRepository, never()).findByIngredientId(any());
    }
}
