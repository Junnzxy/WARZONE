package alp.semester2.tugas.controller;

import alp.semester2.tugas.dto.AuthDto;
import alp.semester2.tugas.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AuthController
 * -----------------------------------------------
 * POST /api/auth/register  → daftar akun baru
 * POST /api/auth/login     → ditangani Spring Security (lihat SecurityConfig)
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthDto.AuthResponse> register(
            @Valid @RequestBody AuthDto.RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }
}
