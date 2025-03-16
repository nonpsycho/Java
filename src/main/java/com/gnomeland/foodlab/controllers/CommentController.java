package com.gnomeland.foodlab.controllers;

import com.gnomeland.foodlab.dto.CommentDto;
import com.gnomeland.foodlab.service.CommentService;
import com.gnomeland.foodlab.service.RecipeService;
import com.gnomeland.foodlab.service.UserService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;
    private final UserService userService;
    private final RecipeService recipeService;

    @Autowired
    public CommentController(CommentService commentService,
                             UserService userService, RecipeService recipeService) {
        this.commentService = commentService;
        this.userService = userService;
        this.recipeService = recipeService;
    }

    @PostMapping
    public CommentDto addComment(@RequestBody CommentDto commentDto) {
        return commentService.addComment(commentDto);
    }

    @GetMapping
    public List<CommentDto> getAllComments() {
        return commentService.getAllComments();
    }

    @GetMapping("/{id}")
    public CommentDto getCommentById(@PathVariable Integer id) {
        return commentService.getCommentById(id);
    }

    @PatchMapping("/{id}")
    public CommentDto patchComment(@PathVariable Integer id,
                                   @RequestBody CommentDto partialCommentDto) {
        return commentService.updateComment(id, partialCommentDto);
    }

    @DeleteMapping("/{id}")
    public void deleteComment(@PathVariable Integer id) {
        commentService.deleteComment(id);
    }

    @GetMapping("/recipe/{recipeId}")
    public List<CommentDto> getCommentsByRecipeId(@PathVariable Integer recipeId) {
        return recipeService.getCommentsByRecipeId(recipeId);
    }

    @GetMapping("/user/{userId}")
    public List<CommentDto> getCommentsByUserId(@PathVariable Integer userId) {
        return userService.getCommentsByUserId(userId);
    }
}