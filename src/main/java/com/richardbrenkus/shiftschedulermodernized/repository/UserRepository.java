package com.richardbrenkus.shiftschedulermodernized.repository;

import java.util.List;
import java.util.Optional;

import com.richardbrenkus.shiftschedulermodernized.entity.User;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    User getUserByUsername(String username);

    Optional<User> findByUsername(String username);

    User getUserById(Long id);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Query("SELECT u FROM User u WHERE u.shiftRequest IS NULL")
    List<User> findUsersWithoutActiveShiftRequest();

    @Modifying
    @Transactional
    @Query("update User u set u.username = :username where u.id = :id")
    void updateUserSetUsernameForId(@Param("username") String username, @Param("id") Long id);

    @Transactional
    @Query("SELECT u FROM User u")
    List<User> findAll(Sort ascending);

}
