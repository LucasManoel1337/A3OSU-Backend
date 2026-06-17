package project.controller;
import jakarta.websocket.server.PathParam;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.modal.dto.*;
import project.service.TorneiosService;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/torneios")
@RequiredArgsConstructor
public class TorneiosController {

    private final TorneiosService torneiosService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TorneioDTO> criar(@ModelAttribute TorneioDTO requestDTO) throws IOException {

        TorneioDTO response = torneiosService.criarTorneio(requestDTO);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TorneioListaDTO>> listarTodosAtivos() {
        return ResponseEntity.ok(torneiosService.listarTodosAtivos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TorneioDetalhesDTO> buscarTorneioPorId(@PathVariable Long id) {
        TorneioDetalhesDTO detalhes = torneiosService.buscarDetalhes(id);
        return ResponseEntity.ok(detalhes);
    }

    @PostMapping("/{id}/entrar")
    public ResponseEntity<String> entrarNoTorneio(@PathVariable Long id, @RequestBody EntrarTorneioDTO requestDTO) {
        try {
            torneiosService.entrarNoTorneio(id, requestDTO);
            return ResponseEntity.ok("Inscrição realizada com sucesso!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}/inscritos")
    public ResponseEntity<List<InscritoDTO>> listarInscritos(@PathVariable Long id) {
        List<InscritoDTO> inscritos = torneiosService.buscarInscritosDoTorneio(id);
        return ResponseEntity.ok(inscritos);
    }

    @GetMapping("/participando/{idUsuario}")
    public ResponseEntity<List<TorneioListaDTO>> listarParticipativos(@PathVariable Long idUsuario) {
        return ResponseEntity.ok(torneiosService.listarParticipativos(idUsuario));
    }

    @GetMapping("/criados/{idUsuario}")
    public ResponseEntity<List<TorneioListaDTO>> listarCriados(@PathVariable Long idUsuario) {
        List<TorneioListaDTO> torneios = torneiosService.listarCriados(idUsuario);
        return ResponseEntity.ok(torneios);
    }

    @GetMapping("/moderando/{idUsuario}")
    public ResponseEntity<List<TorneioListaDTO>> listarModerando(@PathVariable Long idUsuario) {
        List<TorneioListaDTO> torneios = torneiosService.listarModerando(idUsuario);
        return ResponseEntity.ok(torneios);
    }

    @GetMapping("/{id}/moderadores")
    public ResponseEntity<List<InscritoDTO>> listarModeradores(@PathVariable Long id) {
        return ResponseEntity.ok(torneiosService.buscarModeradores(id));
    }

    @PostMapping("/{id}/moderadores/{idModerador}")
    public ResponseEntity<String> adicionarModerador(@PathVariable Long id, @PathVariable Long idModerador) {
        torneiosService.adicionarModerador(id, idModerador);
        return ResponseEntity.ok("Moderador adicionado com sucesso!");
    }

    @DeleteMapping("/{id}/moderadores/{idModerador}")
    public ResponseEntity<String> removerModerador(@PathVariable Long id, @PathVariable Long idModerador) {
        torneiosService.removerModerador(id, idModerador);
        return ResponseEntity.ok("Moderador removido com sucesso!");
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<String> updateStatus(
            @PathVariable Long id,
            @RequestBody StatusUpdateDTO dto,
            Principal principal) {

        try {
            torneiosService.atualizarStatus(id, dto.status(), principal.getName());
            return ResponseEntity.ok("Status atualizado com sucesso");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @PutMapping("/{id}/inscritos/{jogadorId}/pontuacao")
    public ResponseEntity<String> updatePontuacao(
            @PathVariable Long id,
            @PathVariable Long jogadorId,
            @RequestBody PontuacaoUpdateDTO dto,
            Principal principal) {

        try {
            torneiosService.atualizarPontuacao(id, jogadorId, dto.pontuacao(), principal.getName());
            return ResponseEntity.ok("Pontuação atualizada");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }
}