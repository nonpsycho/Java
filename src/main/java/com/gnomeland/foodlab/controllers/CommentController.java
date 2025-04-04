package com.gnomeland.foodlab.controllers;

import com.gnomeland.foodlab.dto.CommentDto;
import com.gnomeland.foodlab.service.CommentService;
import com.gnomeland.foodlab.validation.CommentValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/comments")
@Tag(name = "Comment Controller", description = "API for managing comments")
public class CommentController {

    private final CommentService commentService;

    @Autowired
    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @Operation(summary = "Adding a new comment", description = "Creates a new comment")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
            description = "Comment added successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "404",
                description = "The user or recipe with this ID was not found.")
    })
    @PostMapping
    public ResponseEntity<String> addComment(@RequestBody CommentDto commentDto) {
        CommentValidator.validateCommentDto(commentDto);
        return commentService.addComment(commentDto);
    }

    @Operation(summary = "Output of all comments", description = "Returns all comments")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Comments found"),
        @ApiResponse(responseCode = "404", description = "There are no comments")
    })
    @GetMapping
    public ResponseEntity<List<CommentDto>> getAllComments() {
        List<CommentDto> comments = commentService.getAllComments().getBody();
        return ResponseEntity.ok(comments);
    }

    @Operation(summary = "Getting comments by ID", description = "Returns a comment by its ID")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Comments found"),
        @ApiResponse(responseCode = "404", description = "A comment with this ID was not found.")
    })
    @GetMapping("/{id}")
    public ResponseEntity<CommentDto> getCommentById(@PathVariable Integer id) {
        CommentDto comment = commentService.getCommentById(id).getBody();
        return ResponseEntity.ok(comment);
    }

    @Operation(summary = "Changing a comment", description = "Changes the content of the comment")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
            description = "Comment changed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "404",
                    description = "A comment with this ID was not found.")
    })
    @PutMapping("/{id}")
    public  ResponseEntity<CommentDto> patchComment(@PathVariable Integer id,
                                   @RequestBody CommentDto partialCommentDto) {
        CommentValidator.validateText(partialCommentDto.getText());
        CommentDto updatedComment = commentService.updateComment(id, partialCommentDto);
        return ResponseEntity.ok(updatedComment);
    }

    @Operation(summary = "Deleting a comment", description = "Deleting a comment")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
            description = "Comment deleted successfully"),
        @ApiResponse(responseCode = "404", description = "A comment with this ID was not found.")
    })
    @DeleteMapping("/{id}")
    public void deleteComment(@PathVariable Integer id) {

        commentService.deleteComment(id);
    }
}

