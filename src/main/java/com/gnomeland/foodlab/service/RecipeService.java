package com.gnomeland.foodlab.service;

import com.gnomeland.foodlab.dto.CommentDto;
import com.gnomeland.foodlab.dto.RecipeDto;
import com.gnomeland.foodlab.dto.UserDto;
import com.gnomeland.foodlab.exception.RecipeNotFoundException;
import com.gnomeland.foodlab.exception.UserNotFoundException;
import com.gnomeland.foodlab.model.Comment;
import com.gnomeland.foodlab.model.Recipe;
import com.gnomeland.foodlab.model.User;
import com.gnomeland.foodlab.repository.RecipeRepository;
import com.gnomeland.foodlab.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class RecipeService {
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;

    @Autowired
    public RecipeService(RecipeRepository recipeRepository, UserRepository userRepository) {
        this.recipeRepository = recipeRepository;
        this.userRepository = userRepository;
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

    public RecipeDto addRecipe(RecipeDto recipeDto) {
        Recipe recipe = convertToEntity(recipeDto);
        return convertToDto(recipeRepository.save(recipe));
    }

    @Transactional
    public void deleteRecipeById(Integer id) {
        if (recipeRepository.existsById(id)) {
            recipeRepository.deleteById(id);
        } else {
            throw new RecipeNotFoundException(id);
        }
    }

    @Transactional
    public RecipeDto updateRecipe(Integer id, RecipeDto updatedRecipeDto) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new RecipeNotFoundException(id));

        recipe.setName(updatedRecipeDto.getName());
        recipe.setIngredients(updatedRecipeDto.getIngredients());

        return convertToDto(recipe);
    }

    public RecipeDto patchRecipe(Integer id, RecipeDto partialRecipeDto) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new RecipeNotFoundException(id));

        if (partialRecipeDto.getName() != null) {
            recipe.setName(partialRecipeDto.getName());
        }

        if (partialRecipeDto.getIngredients() != null) {
            recipe.setIngredients(partialRecipeDto.getIngredients());
        }

        return convertToDto(recipeRepository.save(recipe));
    }

    public List<CommentDto> getCommentsByRecipeId(Integer id) {
        Recipe recipe = recipeRepository.findById(id).orElseThrow(()
                -> new RecipeNotFoundException(id));
        return recipe.getComments().stream().map(this::convertToDto).toList();
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

    public List<UserDto> getUsersForRecipe(Integer recipeId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RecipeNotFoundException(recipeId));

        return recipe.getUsers().stream().map(this::convertToDto).toList();
    }

    private RecipeDto convertToDto(Recipe recipe) {
        RecipeDto recipeDto = new RecipeDto();
        recipeDto.setId(recipe.getId());
        recipeDto.setName(recipe.getName());
        recipeDto.setIngredients(recipe.getIngredients());

        if (recipe.getComments() != null) {
            List<CommentDto> commentDtos = recipe.getComments().stream()
                    .map(this::convertToDto)
                    .toList();
            recipeDto.setComments(commentDtos);
        }

        if (recipe.getUsers() != null) {
            List<UserDto> userDtos = recipe.getUsers().stream()
                    .map(this::convertToDto)
                    .toList();
            recipeDto.setUsers(userDtos);
        }

        return recipeDto;
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
        userDto.setPassword(user.getPassword());
        return userDto;
    }

    private Recipe convertToEntity(RecipeDto recipeDto) {
        Recipe recipe = new Recipe();
        recipe.setId(recipeDto.getId());
        recipe.setName(recipeDto.getName());
        recipe.setIngredients(recipeDto.getIngredients());
        return recipe;
    }
}