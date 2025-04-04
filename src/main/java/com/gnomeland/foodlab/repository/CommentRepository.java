package com.gnomeland.foodlab.repository;

import com.gnomeland.foodlab.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Integer> {
    boolean existsByUserIdAndRecipeIdAndText(Integer userId, Integer recipeId, String text);
}
