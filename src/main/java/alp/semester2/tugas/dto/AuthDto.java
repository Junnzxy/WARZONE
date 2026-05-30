package alp.semester2.tugas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class AuthDto {

    // ----- Register -----
    @Data
    public static class RegisterRequest {
        @NotBlank(message = "Nama tidak boleh kosong")
        @Size(min = 2, max = 120)
        private String name;

        @NotBlank(message = "Email tidak boleh kosong")
        @Email(message = "Format email tidak valid")
        private String email;

        @NotBlank(message = "Password tidak boleh kosong")
        @Size(min = 6, message = "Password minimal 6 karakter")
        private String password;
    }

    // ----- Login -----
    @Data
    public static class LoginRequest {
        @NotBlank
        @Email
        private String email;

        @NotBlank
        private String password;
    }

    // ----- Response setelah login/register -----
    @Data
    public static class AuthResponse {
        private Long   id;
        private String name;
        private String email;
        private String role;
        private String message;

        public AuthResponse(Long id, String name, String email, String role, String message) {
            this.id      = id;
            this.name    = name;
            this.email   = email;
            this.role    = role;
            this.message = message;
        }
    }
}
