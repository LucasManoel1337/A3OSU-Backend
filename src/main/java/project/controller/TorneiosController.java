package project.controller;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import project.modal.dto.TorneioDTO;
import project.service.TorneiosService;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/torneios")
@RequiredArgsConstructor
public class TorneiosController {

    private final TorneiosService torneiosService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TorneioDTO> criar(
            @RequestParam("criadorId") Long criadorId,
            @RequestParam("nome") String nome,
            @RequestParam("tipo") String tipo,
            @RequestParam("modo") String modo,
            @RequestParam("vagas") Integer vagas,
            @RequestParam("descricao") String descricao,
            @RequestParam("isPrivado") Boolean isPrivado,
            @RequestParam(value = "senha", required = false) String senha,
            @RequestParam(value = "banner", required = false) MultipartFile banner,
            @RequestParam(value = "logo", required = false) MultipartFile logo) throws IOException { // Trata a exceção aqui

        TorneioDTO response = torneiosService.criarTorneio(
                criadorId, nome, tipo, modo, vagas, descricao, isPrivado, senha, banner, logo
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TorneioDTO>> listarTodos() {
        return ResponseEntity.ok(torneiosService.listarTodos());
    }
}