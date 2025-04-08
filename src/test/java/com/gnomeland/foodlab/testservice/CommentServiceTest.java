package com.gnomeland.foodlab.testservice;

import com.gnomeland.foodlab.dto.CommentDto;
import com.gnomeland.foodlab.exception.CommentException;
import com.gnomeland.foodlab.exception.RecipeException;
import com.gnomeland.foodlab.exception.UserException;
import com.gnomeland.foodlab.model.Comment;
import com.gnomeland.foodlab.repository.CommentRepository;
import com.gnomeland.foodlab.repository.RecipeRepository;
import com.gnomeland.foodlab.repository.UserRepository;
import com.gnomeland.foodlab.service.CommentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CommentService commentService;

    private CommentDto createTestCommentDto() {
        CommentDto dto = new CommentDto();
        dto.setId(1);
        dto.setText("Test comment");
        dto.setUserId(1);
        dto.setRecipeId(1);
        return dto;
    }

    private Comment createTestComment() {
        Comment comment = new Comment();
        comment.setId(1);
        comment.setText("Test comment");
        comment.setUserId(1);
        comment.setRecipeId(1);
        return comment;
    }

    @Test
    void addComment_Success() {
        // Arrange
        CommentDto commentDto = createTestCommentDto();
        when(recipeRepository.existsById(1)).thenReturn(true);
        when(userRepository.existsById(1)).thenReturn(true);
        when(commentRepository.existsByUserIdAndRecipeIdAndText(1, 1, "Test comment")).thenReturn(false);
        when(commentRepository.save(any(Comment.class))).thenReturn(createTestComment());

        // Act
        ResponseEntity<String> response = commentService.addComment(commentDto);

        // Assert
        assertEquals("Comment created successfully", response.getBody());
        verify(commentRepository, times(1)).save(any(Comment.class));
    }

    @Test
    void addComment_RecipeNotFound() {
        // Arrange
        CommentDto commentDto = createTestCommentDto();
        when(recipeRepository.existsById(1)).thenReturn(false);

        // Act & Assert
        RecipeException exception = assertThrows(RecipeException.class,
                () -> commentService.addComment(commentDto));
        assertEquals("The recipe was not found: 1", exception.getMessage());
    }

    @Test
    void addComment_UserNotFound() {
        // Arrange
        CommentDto commentDto = createTestCommentDto();
        when(recipeRepository.existsById(1)).thenReturn(true);
        when(userRepository.existsById(1)).thenReturn(false);

        // Act & Assert
        UserException exception = assertThrows(UserException.class,
                () -> commentService.addComment(commentDto));
        assertEquals("The user was not found: 1", exception.getMessage());
    }

    @Test
    void addComment_DuplicateComment() {
        // Arrange
        CommentDto commentDto = createTestCommentDto();
        when(recipeRepository.existsById(1)).thenReturn(true);
        when(userRepository.existsById(1)).thenReturn(true);
        when(commentRepository.existsByUserIdAndRecipeIdAndText(1, 1, "Test comment")).thenReturn(true);

        // Act & Assert
        CommentException exception = assertThrows(CommentException.class,
                () -> commentService.addComment(commentDto));
        assertEquals("Duplicate comment detected", exception.getMessage());
    }

    @Test
    void getAllComments_Success() {
        // Arrange
        Comment comment = createTestComment();
        when(commentRepository.findAll()).thenReturn(List.of(comment));

        // Act
        ResponseEntity<List<CommentDto>> response = commentService.getAllComments();
        List<CommentDto> body = response.getBody();

        // Assert
        assertNotNull(body);
        assertEquals(1, body.size());
        assertEquals("Test comment", body.get(0).getText());
    }

    @Test
    void getAllComments_EmptyList() {
        // Arrange
        when(commentRepository.findAll()).thenReturn(Collections.emptyList());

        // Act & Assert
        CommentException exception = assertThrows(CommentException.class,
                () -> commentService.getAllComments());
        assertEquals("No comments were found.", exception.getMessage());
    }

    @Test
    void getCommentById_Success() {
        // Arrange
        Comment comment = createTestComment();
        when(commentRepository.findById(1)).thenReturn(Optional.of(comment));

        // Act
        ResponseEntity<CommentDto> response = commentService.getCommentById(1);
        CommentDto body = response.getBody();

        // Assert
        assertNotNull(body);
        assertEquals("Test comment", body.getText());
    }

    @Test
    void getCommentById_NotFound() {
        // Arrange
        when(commentRepository.findById(1)).thenReturn(Optional.empty());

        // Act & Assert
        CommentException exception = assertThrows(CommentException.class,
                () -> commentService.getCommentById(1));
        assertEquals("Comment not found by id: 1", exception.getMessage());
    }

    @Test
    void updateComment_Success() {
        // Arrange
        Comment existingComment = createTestComment();
        CommentDto updateDto = new CommentDto();
        updateDto.setId(1);
        updateDto.setText("Updated comment");

        when(commentRepository.findById(1)).thenReturn(Optional.of(existingComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(existingComment);

        // Act
        CommentDto result = commentService.updateComment(1, updateDto);

        // Assert
        assertEquals("Updated comment", result.getText());
        verify(commentRepository, times(1)).save(existingComment);
    }

    @Test
    void updateComment_NotFound() {
        // Arrange
        when(commentRepository.findById(1)).thenReturn(Optional.empty());

        // Act & Assert
        CommentException exception = assertThrows(CommentException.class,
                () -> commentService.updateComment(1, new CommentDto()));
        assertEquals("Comment not found by id: 1", exception.getMessage());
    }

    @Test
    void deleteComment_Success() {
        // Arrange
        Comment comment = createTestComment();
        when(commentRepository.findById(1)).thenReturn(Optional.of(comment));

        // Act
        commentService.deleteComment(1);

        // Assert
        verify(commentRepository, times(1)).delete(comment);
    }

    @Test
    void deleteComment_NotFound() {
        // Arrange
        when(commentRepository.findById(1)).thenReturn(Optional.empty());

        // Act & Assert
        CommentException exception = assertThrows(CommentException.class,
                () -> commentService.deleteComment(1));
        assertEquals("Comment not found by id: 1", exception.getMessage());
    }
}