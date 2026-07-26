package com.richardbrenkus.shiftschedulermodernized.repository;

import java.util.List;
import java.util.Optional;

import com.richardbrenkus.shiftschedulermodernized.config.constants.Role;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    List<User> findByShiftRequestIsNullOrderByNameAsc();

    List<User> findAllByRoleNotOrderByNameAsc(Role role);

    List<User> findByShiftRequestIsNotNullOrderByNameAsc();

    List<User> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = {
            "shiftRequest",
            "shiftRequest.preferences"
    })
    List<User> findAllByEnabledTrueAndShiftRequestIsNotNullOrderByNameAsc();

    @Query("""
       select distinct u.name
       from User u
       where u.shiftRequest is null
         and u.name is not null
         and u.enabled = true
       order by u.name
       """)
    List<String> findDistinctNamesWithoutShiftRequest();



}
