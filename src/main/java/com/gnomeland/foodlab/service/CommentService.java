package com.gnomeland.foodlab.service;

import com.gnomeland.foodlab.dto.CommentDto;
import com.gnomeland.foodlab.exception.CommentException;
import com.gnomeland.foodlab.exception.RecipeException;
import com.gnomeland.foodlab.exception.UserException;
import com.gnomeland.foodlab.exception.ValidationException;
import com.gnomeland.foodlab.model.Comment;
import com.gnomeland.foodlab.repository.CommentRepository;
import com.gnomeland.foodlab.repository.RecipeRepository;
import com.gnomeland.foodlab.repository.UserRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class CommentService {

    private static final String COMMENT_NOT_FOUND = "Comment not found by id: ";
    private static final String RECIPE_NOT_FOUND_MESSAGE = "The recipe was not found: ";
    private static final String USER_NOT_FOUND_MESSAGE = "The user was not found: ";
    private static final String COMMENT_NOT_BLANK_MESSAGE = "An empty comment";
    private static final String COMMENT_SIZE_MESSAGE = "The length of the comment should be "
            + "from 1 to 300 characters.";

    private final CommentRepository commentRepository;
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;

    @Autowired
    public CommentService(CommentRepository commentRepository,
                          RecipeRepository recipeRepository, UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.recipeRepository = recipeRepository;
        this.userRepository = userRepository;
    }

    public ResponseEntity<String> addComment(CommentDto commentDto) {
        String text = commentDto.getText();

        if (text == null || text.trim().isEmpty()) {
            throw new ValidationException(COMMENT_NOT_BLANK_MESSAGE);
        }

        if (text.length() > 300) {
            throw new ValidationException(COMMENT_SIZE_MESSAGE);
        }

        Integer userId = commentDto.getUserId();
        Integer recipeId = commentDto.getRecipeId();

        if (!recipeRepository.existsById(recipeId)) {
            throw new RecipeException(RECIPE_NOT_FOUND_MESSAGE + recipeId);
        }

        if (!userRepository.existsById(userId)) {
            throw new UserException(USER_NOT_FOUND_MESSAGE + userId);
        }

        boolean commentExists = commentRepository.existsByUserIdAndRecipeIdAndText(userId,
                recipeId, commentDto.getText());
        if (commentExists) {
            throw new CommentException("This user has already left such a comment on this recipe.");
        }

        Comment comment = convertToEntity(commentDto);
        commentRepository.save(comment);

        return ResponseEntity.ok("The comment was created successfully");
    }

    public ResponseEntity<List<CommentDto>> getAllComments() {

        List<CommentDto> comments = commentRepository.findAll().stream()
                .map(this::convertToDto)
                .toList();

        if (comments.isEmpty()) {
            throw new CommentException("No comments were found.");
        }

        return ResponseEntity.ok(comments);
    }

    public ResponseEntity<CommentDto> getCommentById(Integer id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new CommentException(COMMENT_NOT_FOUND + id));
        return ResponseEntity.ok(convertToDto(comment));
    }


    public CommentDto updateComment(Integer id, CommentDto partialCommentDto) {
        Comment existingComment = commentRepository.findById(id)
                .orElseThrow(() -> new CommentException(COMMENT_NOT_FOUND + id));

        String newContent = partialCommentDto.getText();

        if (newContent == null || newContent.trim().isEmpty()
                || newContent.length() > 300) {
            throw new ValidationException("Your " + COMMENT_NOT_BLANK_MESSAGE
                    + " or " + COMMENT_SIZE_MESSAGE);
        }

        existingComment.setText(newContent);

        Comment updatedComment = commentRepository.save(existingComment);

        return convertToDto(updatedComment);
    }

    public void deleteComment(Integer id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new CommentException(COMMENT_NOT_FOUND + id));

        commentRepository.delete(comment);
    }

    private CommentDto convertToDto(Comment comment) {
        CommentDto commentDto = new CommentDto();
        commentDto.setId(comment.getId());
        commentDto.setText(comment.getText());
        commentDto.setUserId(comment.getUserId());
        commentDto.setRecipeId(comment.getRecipeId());
        return commentDto;
    }

    private Comment convertToEntity(CommentDto commentDto) {
        Comment comment = new Comment();
        comment.setId(commentDto.getId());
        comment.setText(commentDto.getText());
        comment.setUserId(commentDto.getUserId());
        comment.setRecipeId(commentDto.getRecipeId());
        return comment;
    }
}