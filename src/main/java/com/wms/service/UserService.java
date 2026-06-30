package com.wms.service;

import com.wms.entity.User;
import com.wms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    // ── Validation helpers ─────────────────────────────────────
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN =
        Pattern.compile("^[+]?[0-9\\s\\-().]{7,20}$");

    private void validateUserFields(User user, boolean requirePassword) {
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new RuntimeException("Username is required.");
        }
        if (user.getUsername().trim().length() < 3) {
            throw new RuntimeException("Username must be at least 3 characters.");
        }
        if (user.getUsername().trim().length() > 50) {
            throw new RuntimeException("Username must not exceed 50 characters.");
        }
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new RuntimeException("Email address is required.");
        }
        if (!EMAIL_PATTERN.matcher(user.getEmail().trim()).matches()) {
            throw new RuntimeException("Please enter a valid email address.");
        }
        if (requirePassword) {
            if (user.getPassword() == null || user.getPassword().isEmpty()) {
                throw new RuntimeException("Password is required.");
            }
            if (user.getPassword().length() < 6) {
                throw new RuntimeException("Password must be at least 6 characters.");
            }
        }
        if (user.getPhone() != null && !user.getPhone().trim().isEmpty()) {
            if (!PHONE_PATTERN.matcher(user.getPhone().trim()).matches()) {
                throw new RuntimeException("Please enter a valid phone number (7-20 digits).");
            }
        }
    }

    public User addUser(User user) {
        validateUserFields(user, true);
        user.setUsername(user.getUsername().trim());
        user.setEmail(user.getEmail().trim().toLowerCase());
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username already exists: " + user.getUsername());
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists: " + user.getEmail());
        }
        return userRepository.save(user);
    }

    public User updateUser(Long id, User updatedUser) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        // Update username only if provided and different
        if (updatedUser.getUsername() != null && !updatedUser.getUsername().isEmpty()) {
            String trimmed = updatedUser.getUsername().trim();
            if (trimmed.length() < 3) throw new RuntimeException("Username must be at least 3 characters.");
            if (!existing.getUsername().equals(trimmed) &&
                    userRepository.existsByUsername(trimmed)) {
                throw new RuntimeException("Username already exists: " + trimmed);
            }
            existing.setUsername(trimmed);
        }

        // Update email only if provided and different
        if (updatedUser.getEmail() != null && !updatedUser.getEmail().isEmpty()) {
            String trimmedEmail = updatedUser.getEmail().trim().toLowerCase();
            if (!EMAIL_PATTERN.matcher(trimmedEmail).matches()) {
                throw new RuntimeException("Please enter a valid email address.");
            }
            if (!existing.getEmail().equals(trimmedEmail) &&
                    userRepository.existsByEmail(trimmedEmail)) {
                throw new RuntimeException("Email already exists: " + trimmedEmail);
            }
            existing.setEmail(trimmedEmail);
        }

        if (updatedUser.getFullName() != null && !updatedUser.getFullName().isEmpty()) {
            existing.setFullName(updatedUser.getFullName().trim());
        }

        if (updatedUser.getProfilePic() != null && !updatedUser.getProfilePic().isEmpty()) {
            existing.setProfilePic(updatedUser.getProfilePic());
        }

        if (updatedUser.getPhone() != null && !updatedUser.getPhone().isEmpty()) {
            String phone = updatedUser.getPhone().trim();
            if (!PHONE_PATTERN.matcher(phone).matches()) {
                throw new RuntimeException("Please enter a valid phone number (7-20 digits).");
            }
            existing.setPhone(phone);
        }

        if (updatedUser.getRole() != null && !updatedUser.getRole().isEmpty()) {
            existing.setRole(updatedUser.getRole());
        }

        // Update password only if provided
        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
            if (updatedUser.getPassword().length() < 6) {
                throw new RuntimeException("Password must be at least 6 characters.");
            }
            existing.setPassword(updatedUser.getPassword());
        }

        existing.setActive(updatedUser.isActive());

        return userRepository.save(existing);
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    public List<User> searchUsers(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return userRepository.findAll();
        }
        return userRepository.searchUsers(keyword.trim());
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public long getTotalUsers() {
        return userRepository.count();
    }

    public long getActiveUsers() {
        return userRepository.findByActive(true).size();
    }
}