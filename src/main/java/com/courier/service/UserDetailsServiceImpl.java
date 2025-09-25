package com.courier.service;

import com.courier.entity.User;
import com.courier.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserDetailsServiceImpl.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.debug("Loading user by username: {}", username);
        
        User user = userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> {
                    logger.error("User not found with username or email: {}", username);
                    return new UsernameNotFoundException("User not found with username or email: " + username);
                });
        
        logger.debug("User found: {} with roles: {}", user.getUsername(), user.getRoles());
        
        return user;
    }
    
    @Transactional
    public UserDetails loadUserById(Long id) {
        logger.debug("Loading user by ID: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("User not found with ID: {}", id);
                    return new UsernameNotFoundException("User not found with ID: " + id);
                });
        
        logger.debug("User found by ID: {}", user.getUsername());
        
        return user;
    }
}
