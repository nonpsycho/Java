package com.gnomeland.foodlab.service;

import com.gnomeland.foodlab.dto.CommentDto;
import com.gnomeland.foodlab.dto.RecipeDto;
import com.gnomeland.foodlab.dto.UserDto;
import com.gnomeland.foodlab.exception.UserException;
import com.gnomeland.foodlab.model.Comment;
import com.gnomeland.foodlab.model.Recipe;
import com.gnomeland.foodlab.model.User;
import com.gnomeland.foodlab.repository.RecipeRepository;
import com.gnomeland.foodlab.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private static final String USER_NOT_FOUND_MESSAGE = "The user was not found: ";
    private static final String USER_EXISTS_MESSAGE =
            "A user with the same name and email address already exists: ";

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
        if (users.isEmpty()) {
            throw new UserException(USER_NOT_FOUND_MESSAGE
                    + "There is no user with these parameters.");
        }
        return users.stream().map(this::convertToDto).toList();
    }

    public UserDto getUserById(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND_MESSAGE + id));
        return convertToDto(user);
    }

    public UserDto addUser(UserDto userDto) {
        User user = convertToEntity(userDto);
        if (!userRepository.findByUsernameIgnoreCase(user.getUsername()).isEmpty()) {
            throw new IllegalArgumentException(USER_EXISTS_MESSAGE + user.getUsername());
        }
        if (!userRepository.findByEmailIgnoreCase(user.getEmail()).isEmpty()) {
            throw new IllegalArgumentException(USER_EXISTS_MESSAGE + user.getEmail());
        }
        User savedUser = userRepository.save(user);

        return convertToDto(savedUser);
    }

    @Transactional
    public ResponseEntity<String> deleteUserById(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND_MESSAGE + id));

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
                .orElseThrow(() -> new UserException(USER_NOT_FOUND_MESSAGE + id));

        user.setUsername(updatedUserDto.getUsername());
        user.setEmail(updatedUserDto.getEmail());
        user.setPassword(updatedUserDto.getPassword());

        User updatedUser = userRepository.save(user);

        return convertToDto(updatedUser);
    }

    public UserDto patchUser(Integer id, UserDto partialUserDto) {
        if (partialUserDto == null) {
            partialUserDto = new UserDto(); // Обрабатываем null случай
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND_MESSAGE + id));

        if (partialUserDto.getUsername() != null) {
            user.setUsername(partialUserDto.getUsername());
        }
        if (partialUserDto.getEmail() != null) {
            user.setEmail(partialUserDto.getEmail());
        }
        if (partialUserDto.getPassword() != null) {
            user.setPassword(partialUserDto.getPassword());
        }

        User updatedUser = userRepository.save(user);

        return convertToDto(updatedUser);
    }

    public List<CommentDto> getCommentsByUserId(Integer id) {
        User user = userRepository.findById(id).orElseThrow(()
                -> new UserException(USER_NOT_FOUND_MESSAGE + id));
        return user.getComments().stream().map(this::convertToDto).toList();
    }

    private UserDto convertToDto(User user) {
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setUsername(user.getUsername());
        userDto.setEmail(user.getEmail());

        if (user.getComments() != null) {
            List<CommentDto> commentDtos = user.getComments().stream()
                    .map(this::convertToDto)
                    .toList();
            userDto.setComments(commentDtos);
        }

        if (user.getSavedRecipes() != null) {
            List<RecipeDto> recipeDtos = user.getSavedRecipes().stream()
                    .map(this::convertToDto)
                    .toList();
            userDto.setSavedRecipes(recipeDtos);
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
