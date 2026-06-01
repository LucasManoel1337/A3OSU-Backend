package project.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import project.modal.dto.ChangePasswordDTO;
import project.modal.dto.UpdateProfileDTO;
import project.modal.dto.UserProfileDTO;
import project.modal.entity.User;
import project.modal.entity.UserDetalhes;
import project.service.UserService;

import java.io.IOException;

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

    @PostMapping("/upload/{type}")
    public ResponseEntity<String> upload(@PathVariable String type,
                                         @RequestParam("file") MultipartFile file,
                                         @AuthenticationPrincipal User user) throws IOException {
        userService.updateAsset(user.getId(), file, type);
        return ResponseEntity.ok("Upload realizado com sucesso.");
    }

    @GetMapping("/{userId}/avatar")
    public ResponseEntity<byte[]> getAvatar(@PathVariable Long userId) {
        UserDetalhes detalhes = userService.getDetalhesByUserId(userId);

        // Verifique se existe dado antes de retornar
        if (detalhes.getAvatarData() == null) return ResponseEntity.notFound().build();

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(detalhes.getAvatarData());
    }

    @GetMapping("/{userId}/banner")
    public ResponseEntity<byte[]> getBanner(@PathVariable Long userId) {
        UserDetalhes detalhes = userService.getDetalhesByUserId(userId);

        if (detalhes.getBannerData() == null) return ResponseEntity.notFound().build();

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(detalhes.getBannerData());
    }
}