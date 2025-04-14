package com.gnomeland.foodlab.testservice;

import com.gnomeland.foodlab.dto.*;
import com.gnomeland.foodlab.exception.*;
import com.gnomeland.foodlab.model.*;
import com.gnomeland.foodlab.repository.*;
import com.gnomeland.foodlab.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RecipeRepository recipeRepository;

    @InjectMocks
    private UserService userService;

    private User user;
    private UserDto userDto;
    private Recipe recipe;
    private Comment comment;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("password");
        user.setSavedRecipes(new HashSet<>());
        user.setComments(new ArrayList<>());

        userDto = new UserDto();
        userDto.setId(1);
        userDto.setUsername("testuser");
        userDto.setEmail("test@example.com");
        userDto.setPassword("password");

        recipe = new Recipe();
        recipe.setId(1);
        recipe.setName("Test Recipe");

        comment = new Comment();
        comment.setId(1);
        comment.setText("Test comment");
        comment.setUserId(1);
        comment.setRecipeId(1);
    }

    // Тесты для getUsers()
    @Test
    void getUsers_shouldReturnAllUsers_whenNoFilters() {
        // Arrange
        when(userRepository.findAll()).thenReturn(List.of(user));

        // Act
        List<UserDto> result = userService.getUsers(null, null);

        // Assert
        assertEquals(1, result.size());
        assertEquals("testuser", result.getFirst().getUsername());
    }

    @Test
    void getUsers_shouldFilterByUsername() {
        // Arrange
        when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(List.of(user));

        // Act
        List<UserDto> result = userService.getUsers("testuser", null);

        // Assert
        assertEquals(1, result.size());
    }

    @Test
    void getUsers_shouldFilterByEmail() {
        // Arrange
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(List.of(user));

        // Act
        List<UserDto> result = userService.getUsers(null, "test@example.com");

        // Assert
        assertEquals(1, result.size());
    }

    @Test
    void getUsers_shouldFilterByUsernameAndEmail() {
        // Arrange
        when(userRepository.findByUsernameIgnoreCaseAndEmailIgnoreCase("testuser", "test@example.com"))
                .thenReturn(List.of(user));

        // Act
        List<UserDto> result = userService.getUsers("testuser", "test@example.com");

        // Assert
        assertEquals(1, result.size());
    }

    @Test
    void getUsers_shouldThrowException_whenNoUsersFound() {
        // Arrange
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        // Act & Assert
        assertThrows(UserException.class, () -> userService.getUsers(null, null));
    }

    // Тесты для getUserById()
    @Test
    void getUserById_shouldReturnUser_whenExists() {
        // Arrange
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        // Act
        UserDto result = userService.getUserById(1);

        // Assert
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void getUserById_shouldThrowException_whenNotFound() {
        // Arrange
        when(userRepository.findById(1)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserException.class, () -> userService.getUserById(1));
    }

    // Тесты для addUser()
    @Test
    void addUser_shouldSaveNewUser() {
        // Arrange
        when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Collections.emptyList());
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Collections.emptyList());
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        UserDto result = userService.addUser(userDto);

        // Assert
        assertEquals("testuser", result.getUsername());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void addUser_shouldThrowException_whenUsernameExists() {
        // Arrange
        when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(List.of(user));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> userService.addUser(userDto));
    }

    @Test
    void addUser_shouldThrowException_whenEmailExists() {
        // Arrange
        when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Collections.emptyList());
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(List.of(user));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> userService.addUser(userDto));
    }

    // Тесты для deleteUserById()
    @Test
    void deleteUserById_shouldDeleteUserAndCleanRelations() {
        // Arrange
        user.getSavedRecipes().add(recipe);
        recipe.getUsers().add(user);

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(recipeRepository.save(recipe)).thenReturn(recipe);

        // Act
        ResponseEntity<String> response = userService.deleteUserById(1);

        // Assert
        assertEquals(204, response.getStatusCode().value());
        verify(userRepository).deleteById(1);
        verify(recipeRepository).save(recipe);
    }

    @Test
    void deleteUserById_shouldThrowException_whenUserNotFound() {
        // Arrange
        when(userRepository.findById(1)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserException.class, () -> userService.deleteUserById(1));
    }

    // Тесты для updateUser()
    @Test
    void updateUser_shouldUpdateAllFields() {
        // Arrange
        UserDto updatedDto = new UserDto();
        updatedDto.setUsername("newuser");
        updatedDto.setEmail("new@example.com");
        updatedDto.setPassword("newpassword");

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        // Act
        UserDto result = userService.updateUser(1, updatedDto);

        // Assert
        assertEquals("newuser", result.getUsername());
        assertEquals("new@example.com", result.getEmail());
    }

    @Test
    void updateUser_shouldThrowException_whenUserNotFound() {
        // Arrange
        when(userRepository.findById(1)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserException.class, () -> userService.updateUser(1, userDto));
    }

    // Тесты для patchUser()
    @Test
    void patchUser_shouldUpdateOnlyProvidedFields() {
        // Arrange
        UserDto partialDto = new UserDto();
        partialDto.setUsername("updateduser");

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        // Act
        UserDto result = userService.patchUser(1, partialDto);

        // Assert
        assertEquals("updateduser", result.getUsername());
        assertEquals("test@example.com", result.getEmail()); // Осталось без изменений
    }

    @Test
    void patchUser_shouldThrowException_whenUserNotFound() {
        // Arrange
        when(userRepository.findById(1)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserException.class, () -> userService.patchUser(1, userDto));
    }

    // Тесты для getCommentsByUserId()
    @Test
    void getCommentsByUserId_shouldReturnComments() {
        // Arrange
        user.getComments().add(comment);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        // Act
        List<CommentDto> result = userService.getCommentsByUserId(1);

        // Assert
        assertEquals(1, result.size());
        assertEquals("Test comment", result.getFirst().getText());
    }

    @Test
    void getCommentsByUserId_shouldThrowException_whenUserNotFound() {
        // Arrange
        when(userRepository.findById(1)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserException.class, () -> userService.getCommentsByUserId(1));
    }

    // Тесты для convertToDto()
    @Test
    void getUserById_shouldReturnConvertedUserDto() {
        // Arrange
        user.getSavedRecipes().add(recipe);
        user.getComments().add(comment);

        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        // Act
        UserDto result = userService.getUserById(1);

        // Assert
        assertEquals("testuser", result.getUsername());
        assertEquals(1, result.getSavedRecipes().size());
        assertEquals(1, result.getComments().size());
    }

    @Test
    void patchUser_shouldNotModifyOtherUserFields() {
        // Arrange
        UserDto partialDto = new UserDto();
        partialDto.setUsername("newusername");

        Set<Recipe> savedRecipes = new HashSet<>(user.getSavedRecipes());
        List<Comment> comments = new ArrayList<>(user.getComments());

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        // Act
        userService.patchUser(1, partialDto); // Убрано присваивание result

        // Assert
        assertEquals(savedRecipes, user.getSavedRecipes());
        assertEquals(comments, user.getComments());
    }

    @Test
    void patchUser_shouldNotModifyOtherUserFields2() {
        UserDto partialUserDto = new UserDto();
        partialUserDto.setEmail("shizognome@gmail.com");

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        UserDto patchedUser = userService.patchUser(1, partialUserDto);

        assertNotNull(patchedUser);
        assertEquals("shizognome@gmail.com", patchedUser.getEmail());
    }

    @Test
    void patchUser_shouldNotModifyOtherUserFields3() {
        UserDto partialUserDto = new UserDto();
        partialUserDto.setPassword("email1323");

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        UserDto patchedUser = userService.patchUser(1, partialUserDto);

        assertNotNull(patchedUser);
        assertNull(patchedUser.getPassword());
    }


}

