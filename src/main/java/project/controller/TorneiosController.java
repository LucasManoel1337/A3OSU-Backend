package project.controller;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.modal.dto.*;
import project.service.TorneiosService;

import java.io.IOException;
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
    public ResponseEntity<List<TorneioListaDTO>> listarTodos() {
        return ResponseEntity.ok(torneiosService.listarTodos());
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
}