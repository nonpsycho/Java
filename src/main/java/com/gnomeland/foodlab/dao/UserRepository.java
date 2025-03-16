package com.gnomeland.foodlab.dao;

import com.gnomeland.foodlab.model.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    List<User> findByUsernameIgnoreCase(String name);

    List<User> findByEmailIgnoreCase(String email);

    List<User> findByUsernameIgnoreCaseAndEmailIgnoreCase(String name, String email);

}

