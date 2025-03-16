package com.gnomeland.foodlab.service;

import com.gnomeland.foodlab.dao.RecipeRepository;
import com.gnomeland.foodlab.dao.UserRepository;
import com.gnomeland.foodlab.dto.CommentDto;
import com.gnomeland.foodlab.dto.RecipeDto;
import com.gnomeland.foodlab.dto.UserDto;
import com.gnomeland.foodlab.exception.UserNotFoundException;
import com.gnomeland.foodlab.model.Comment;
import com.gnomeland.foodlab.model.Recipe;
import com.gnomeland.foodlab.model.User;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;

    @Autowired
    public UserService(UserRepository userRepository, RecipeRepository recipeRepository) {
        this.userRepository = userRepository;
        this.recipeRepository = recipeRepository;
    }

    public List<UserDto> getUsers(String userName, String email) {
        List<User> users;
        if (userName != null && email != null) {
            users = userRepository.findByUsernameIgnoreCaseAndEmailIgnoreCase(userName, email);
        } else if (userName != null) {
            users = userRepository.findByUsernameIgnoreCase(userName);
        } else if (email != null) {
            users = userRepository.findByEmailIgnoreCase(email);
        } else {
            users = userRepository.findAll();
        }
        return users.stream().map(this::convertToDto).toList();
    }

    public UserDto getUserById(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return convertToDto(user);
    }

    public UserDto addUser(UserDto userDto) {
        User user = convertToEntity(userDto);
        if (!userRepository.findByUsernameIgnoreCase(user.getUsername()).isEmpty()) {
            throw new IllegalArgumentException("User with the same name already exist"
                    + user.getUsername());
        }
        if (!userRepository.findByEmailIgnoreCase(user.getEmail()).isEmpty()) {
            throw new IllegalArgumentException("User with the same email already exist"
                    + user.getEmail());
        }
        return convertToDto(userRepository.save(user));
    }

    @Transactional
    public ResponseEntity<Void> deleteUserById(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        for (Recipe recipe : user.getSavedRecipes()) {
            recipe.getUsers().remove(user);
            recipeRepository.save(recipe);
        }

        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @Transactional
    public UserDto updateUser(Integer id, UserDto updatedUserDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        user.setUsername(updatedUserDto.getUsername());
        user.setEmail(updatedUserDto.getEmail());
        user.setPassword(updatedUserDto.getPassword());

        return convertToDto(user);
    }

    public UserDto patchUser(Integer id, UserDto partialUserDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        if (partialUserDto.getUsername() != null) {
            user.setUsername(partialUserDto.getUsername());
        }
        if (partialUserDto.getEmail() != null) {
            user.setEmail(partialUserDto.getEmail());
        }
        if (partialUserDto.getPassword() != null) {
            user.setPassword(partialUserDto.getPassword());
        }

        return convertToDto(userRepository.save(user));
    }

    public List<CommentDto> getCommentsByUserId(Integer id) {
        User user = userRepository.findById(id).orElseThrow(()
                -> new UserNotFoundException(id));
        return user.getComments().stream().map(this::convertToDto).toList();
    }

    private UserDto convertToDto(User user) {
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setUsername(user.getUsername());
        userDto.setEmail(user.getEmail());
        userDto.setPassword(user.getPassword());

        if (user.getComments() != null) {
            List<CommentDto> commentDtos = user.getComments().stream()
                    .map(this::convertToDto)
                    .toList();
            userDto.setComments(commentDtos);
        }

        if (user.getSavedRecipes() != null) {
            List<RecipeDto> movieDtos = user.getSavedRecipes().stream()
                    .map(this::convertToDto)
                    .toList();
            userDto.setSavedRecipes(movieDtos);
        }

        return userDto;
    }

    private CommentDto convertToDto(Comment comment) {
        CommentDto commentDto = new CommentDto();
        commentDto.setId(comment.getId());
        commentDto.setText(comment.getText());
        commentDto.setUserId(comment.getUserId());
        commentDto.setRecipeId(comment.getRecipeId());
        return commentDto;
    }

    private RecipeDto convertToDto(Recipe recipe) {
        RecipeDto recipeDto = new RecipeDto();
        recipeDto.setId(recipe.getId());
        recipeDto.setName(recipe.getName());
        recipeDto.setIngredients(recipe.getIngredients());
        return recipeDto;
    }

    private User convertToEntity(UserDto userDto) {
        User user = new User();
        user.setId(userDto.getId());
        user.setUsername(userDto.getUsername());
        user.setEmail(userDto.getEmail());
        user.setPassword(userDto.getPassword());
        return user;
    }
}