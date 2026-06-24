package com.richardbrenkus.shiftschedulermodernized.repository;

import java.util.List;

import com.richardbrenkus.shiftschedulermodernized.entity.User;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UserRepository extends CrudRepository<User, Long> {

    User getUserByUsername(String username);

    User getUserById(Long id);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByNameIgnoreCase(String name);

    @Modifying
    @Transactional
    @Query("update User u set u.id = :id where u.username = :username")
    void updateUserSetIdForUsername(@Param("id") Long id, @Param("username") String username);

    @Transactional
    @Query("SELECT u FROM User u")
    List<User> findAll(Sort ascending);

}
