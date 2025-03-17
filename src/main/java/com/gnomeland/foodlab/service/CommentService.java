package com.gnomeland.foodlab.service;

import com.gnomeland.foodlab.dto.CommentDto;
import com.gnomeland.foodlab.exception.CommentNotFoundException;
import com.gnomeland.foodlab.exception.RecipeNotFoundException;
import com.gnomeland.foodlab.exception.UserNotFoundException;
import com.gnomeland.foodlab.model.Comment;
import com.gnomeland.foodlab.repository.CommentRepository;
import com.gnomeland.foodlab.repository.RecipeRepository;
import com.gnomeland.foodlab.repository.UserRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CommentService {
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

    public CommentDto addComment(CommentDto commentDto) {
        Integer userId = commentDto.getUserId();
        Integer recipeId = commentDto.getRecipeId();

        validateUserExists(userId);
        validateRecipeExists(recipeId);

        Comment comment = convertToEntity(commentDto);
        return convertToDto(commentRepository.save(comment));
    }

    public List<CommentDto> getAllComments() {
        return commentRepository.findAll().stream().map(this::convertToDto).toList();
    }

    public CommentDto getCommentById(Integer id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new CommentNotFoundException(id));
        return convertToDto(comment);
    }

    public CommentDto updateComment(Integer id, CommentDto partialCommentDto) {
        Comment existingComment = commentRepository.findById(id)
                .orElseThrow(() -> new CommentNotFoundException(id));

        if (partialCommentDto.getText() != null) {
            existingComment.setText(partialCommentDto.getText());
        }

        return convertToDto(commentRepository.save(existingComment));
    }

    public void deleteComment(Integer id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new CommentNotFoundException(id));
        commentRepository.delete(comment);
    }

    private void validateRecipeExists(Integer recipeId) {
        if (!recipeRepository.existsById(recipeId)) {
            throw new RecipeNotFoundException(recipeId);
        }
    }

    private void validateUserExists(Integer userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
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