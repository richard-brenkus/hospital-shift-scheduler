package com.richardbrenkus.hospitalshiftscheduler.service;

import com.richardbrenkus.hospitalshiftscheduler.entity.User;
import com.richardbrenkus.hospitalshiftscheduler.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isEmpty()) {
            throw new UsernameNotFoundException("Invalid username: " + username);
        }

        User user = userOptional.get();

        return org.springframework.security.core.userdetails.User.withUsername(user.getUsername()).password(user.getPassword()).roles(user.getRole().name()).build();
    }
}
