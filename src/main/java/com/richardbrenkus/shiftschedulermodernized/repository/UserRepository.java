package com.richardbrenkus.shiftschedulermodernized.repository;

import java.util.List;
import java.util.Optional;

import com.richardbrenkus.shiftschedulermodernized.config.constants.Role;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

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

    List<User> findAllByRoleNotOrderByNameAsc(Role role);

    @Query("""
    select u
    from User u
    where u.shiftRequest is not null
    order by u.name
    """)
    List<User> findUsersWithShiftRequest();

    List<User> findAllByOrderByNameAsc();

}
