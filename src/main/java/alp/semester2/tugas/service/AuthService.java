package alp.semester2.tugas.service;

import alp.semester2.tugas.dto.AuthDto;
import alp.semester2.tugas.model.User;
import alp.semester2.tugas.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository  userRepo;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo        = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthDto.AuthResponse register(AuthDto.RegisterRequest req) {
        if (userRepo.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email sudah terdaftar.");
        }

        User user = new User();
        user.setName(req.getName());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole(User.Role.user);
        userRepo.save(user);

        return new AuthDto.AuthResponse(
            user.getId(), user.getName(), user.getEmail(),
            user.getRole().name(), "Registrasi berhasil."
        );
    }

    public User loadByEmail(String email) {
        return userRepo.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User tidak ditemukan."));
    }
}
