package com.courier.controller;

import com.courier.dto.JwtResponse;
import com.courier.dto.LoginRequest;
import com.courier.dto.SignupRequest;
import com.courier.entity.User;
import com.courier.repository.UserRepository;
import com.courier.security.JwtUtils;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder encoder;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @PostMapping("/signin")
    public ResponseEntity<Map<String, Object>> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        logger.info("Authentication attempt for user: {}", loginRequest.getUsernameOrEmail());
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsernameOrEmail(), 
                    loginRequest.getPassword())
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            User userPrincipal = (User) authentication.getPrincipal();
            String jwt = jwtUtils.generateJwtToken(authentication);
            String refreshToken = jwtUtils.generateRefreshToken(userPrincipal);
            
            // Update last login time
            userRepository.updateLastLogin(userPrincipal.getId(), LocalDateTime.now());
            
            JwtResponse jwtResponse = new JwtResponse(
                jwt,
                refreshToken,
                userPrincipal.getId(),
                userPrincipal.getUsername(),
                userPrincipal.getEmail(),
                userPrincipal.getFirstName(),
                userPrincipal.getLastName(),
                userPrincipal.getRoles()
            );
            
            response.put("success", true);
            response.put("message", "User authenticated successfully");
            response.put("user", jwtResponse);
            
            logger.info("User {} authenticated successfully", userPrincipal.getUsername());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Authentication failed for user: {}", loginRequest.getUsernameOrEmail(), e);
            
            response.put("success", false);
            response.put("message", "Invalid username/email or password");
            response.put("error", "Authentication failed");
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
    
    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        logger.info("Registration attempt for user: {}", signUpRequest.getUsername());
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Check if username already exists
            if (userRepository.existsByUsername(signUpRequest.getUsername())) {
                response.put("success", false);
                response.put("message", "Username is already taken!");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Check if email already exists
            if (userRepository.existsByEmail(signUpRequest.getEmail())) {
                response.put("success", false);
                response.put("message", "Email is already in use!");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Create new user
            User user = new User(
                signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getPassword()),
                signUpRequest.getFirstName(),
                signUpRequest.getLastName()
            );
            
            user.setPhone(signUpRequest.getPhone());
            
            // Set roles - default to CUSTOMER if not specified
            List<User.Role> roles = signUpRequest.getRoles();
            if (roles == null || roles.isEmpty()) {
                roles = List.of(User.Role.CUSTOMER);
            }
            user.setRoles(roles);
            
            User savedUser = userRepository.save(user);
            
            response.put("success", true);
            response.put("message", "User registered successfully!");
            response.put("userId", savedUser.getId());
            response.put("username", savedUser.getUsername());
            response.put("email", savedUser.getEmail());
            response.put("roles", savedUser.getRoles());
            
            logger.info("User {} registered successfully with roles: {}", savedUser.getUsername(), savedUser.getRoles());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Registration failed for user: {}", signUpRequest.getUsername(), e);
            
            response.put("success", false);
            response.put("message", "Registration failed: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Refresh token is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (!jwtUtils.validateJwtToken(refreshToken) || !jwtUtils.isRefreshToken(refreshToken)) {
                response.put("success", false);
                response.put("message", "Invalid refresh token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            String username = jwtUtils.getUserNameFromJwtToken(refreshToken);
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            String newAccessToken = jwtUtils.generateTokenFromUser(user);
            
            response.put("success", true);
            response.put("message", "Token refreshed successfully");
            response.put("accessToken", newAccessToken);
            response.put("tokenType", "Bearer");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Token refresh failed", e);
            
            response.put("success", false);
            response.put("message", "Token refresh failed: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
    
    @PostMapping("/signout")
    public ResponseEntity<Map<String, Object>> logoutUser() {
        SecurityContextHolder.clearContext();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "User signed out successfully!");
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                response.put("success", false);
                response.put("message", "User not authenticated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            User user = (User) authentication.getPrincipal();
            
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("email", user.getEmail());
            userInfo.put("firstName", user.getFirstName());
            userInfo.put("lastName", user.getLastName());
            userInfo.put("phone", user.getPhone());
            userInfo.put("roles", user.getRoles());
            userInfo.put("enabled", user.isEnabled());
            userInfo.put("createdAt", user.getCreatedAt());
            userInfo.put("lastLogin", user.getLastLogin());
            
            response.put("success", true);
            response.put("message", "User information retrieved successfully");
            response.put("user", userInfo);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving current user information", e);
            
            response.put("success", false);
            response.put("message", "Error retrieving user information: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestHeader("Authorization") String token) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (token != null && token.startsWith("Bearer ")) {
                String jwt = token.substring(7);
                boolean isValid = jwtUtils.validateJwtToken(jwt);
                
                response.put("success", true);
                response.put("valid", isValid);
                
                if (isValid) {
                    String username = jwtUtils.getUserNameFromJwtToken(jwt);
                    response.put("username", username);
                    response.put("message", "Token is valid");
                } else {
                    response.put("message", "Token is invalid or expired");
                }
            } else {
                response.put("success", false);
                response.put("valid", false);
                response.put("message", "Invalid token format");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Token validation error", e);
            
            response.put("success", false);
            response.put("valid", false);
            response.put("message", "Token validation failed: " + e.getMessage());
            
            return ResponseEntity.ok(response);
        }
    }
}
