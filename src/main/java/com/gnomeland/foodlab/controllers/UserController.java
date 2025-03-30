package com.gnomeland.foodlab.controllers;

import com.gnomeland.foodlab.dto.CommentDto;
import com.gnomeland.foodlab.dto.UserDto;
import com.gnomeland.foodlab.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Controller", description = "API for user management")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "User search by filter",
            description = "Returns all the user's name or email address")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
            description = "The found users are returned"),
        @ApiResponse(responseCode = "404", description = "User not found"),
    })
    @GetMapping
    public  ResponseEntity<List<UserDto>> getUsers(
            @RequestParam(name = "username", required = false) String username,
            @RequestParam(name = "email", required = false) String email) {
        List<UserDto> users = userService.getUsers(username, email);
        return users.isEmpty() ? ResponseEntity.status(404).body(users) : ResponseEntity.ok(users);
    }


    @Operation(summary = "Search for a user by ID", description = "Returns the user by his ID")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
            description = "The user has been found"),
        @ApiResponse(responseCode = "404", description = "There is no such user")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Integer id) {
        UserDto userDto = userService.getUserById(id);
        return ResponseEntity.ok(userDto);
    }

    @Operation(summary = "Getting user comments by their ID",
            description = "Returns all comments from the user with the ID")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Comments returned"),
        @ApiResponse(responseCode = "404", description = "The user with this ID was not found.")
    })
    @GetMapping("/{id}/comments")
    public ResponseEntity<List<CommentDto>> getCommentsByUserId(@PathVariable final Integer id) {
        List<CommentDto> comments = userService.getCommentsByUserId(id);
        return ResponseEntity.ok(comments);
    }

    @Operation(summary = "Adding a new user",
            description = "Adds a new user")
    @ApiResponses(value = { @ApiResponse(responseCode = "201",
            description = "The user was added successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "404", description = "Such a user already exists")
    })
    @PostMapping
    public ResponseEntity<UserDto> addUser(@Valid @RequestBody UserDto userDto) {
        UserDto addedUser = userService.addUser(userDto);
        return ResponseEntity.status(201).body(addedUser);
    }


    @Operation(summary = "Deleting a user", description = "Deletes a user by their ID")
    @ApiResponses(value = { @ApiResponse(responseCode = "204",
            description = "The user was deleted successfully"),
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Integer id) {
        return userService.deleteUserById(id);
    }

    @Operation(summary = "Changing the user",
            description = "Modifies all user information")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
            description = "The user was changed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
    })
    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(@PathVariable Integer id,
                                              @Valid @RequestBody UserDto updatedUserDto) {
        UserDto updatedUser = userService.updateUser(id, updatedUserDto);
        return ResponseEntity.ok(updatedUser);
    }

    @Operation(summary = "Changing the user",
            description = "Modifies user information")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
            description = "The user was changed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
    })
    @PatchMapping("/{id}")
    public ResponseEntity<UserDto> patchUser(@PathVariable Integer id,
                                             @Valid @RequestBody UserDto partialUserDto) {
        UserDto updatedUser = userService.patchUser(id, partialUserDto);
        return ResponseEntity.ok(updatedUser);
    }
}