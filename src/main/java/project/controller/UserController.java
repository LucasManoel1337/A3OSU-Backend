package project.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import project.modal.dto.ChangePasswordDTO;
import project.modal.dto.UpdateProfileDTO;
import project.modal.dto.UserProfileDTO;
import project.service.UserService;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Endpoint para atualizar perfil (Nacionalidade e Idioma)
    @PutMapping("/profile")
    public ResponseEntity<String> updateProfile(@RequestBody UpdateProfileDTO dto, Authentication authentication) {
        // authentication.getName() extrai o Username de dentro do JWT logado
        String username = authentication.getName();
        userService.updateProfile(username, dto);
        return ResponseEntity.ok("Perfil atualizado com sucesso!");
    }

    // Endpoint para alterar a senha
    @PutMapping("/change-password")
    public ResponseEntity<String> changePassword(@RequestBody ChangePasswordDTO dto, Authentication authentication) {
        String username = authentication.getName();
        userService.changePassword(username, dto);
        return ResponseEntity.ok("Senha alterada com sucesso!");
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> getMyProfile(Authentication authentication) {
        String username = authentication.getName(); // Extrai o nick direto do JWT
        UserProfileDTO profile = userService.getMyProfile(username);
        return ResponseEntity.ok(profile);
    }
}