package project.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import project.modal.dto.EntrarTorneioDTO;
import project.modal.entity.Torneio;
import project.modal.entity.TorneioInscricao;
import project.modal.entity.User;
import project.modal.entity.UserDetalhes;
import project.repository.InscricaoTorneioRepository;
import project.repository.TorneiosRepository;
import project.repository.UserDetalhesRepository;

import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TorneiosServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private TorneiosRepository torneioRepository;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Mock
    private UserDetalhesRepository userDetalhesRepository;

    @Mock
    private InscricaoTorneioRepository inscricaoRepository;

    @InjectMocks
    private TorneiosService torneiosService;

    @Test
    void joinsTournamentUsingAuthenticatedUserInsteadOfRequestUserId() {
        User actor = new User();
        actor.setId(42L);
        actor.setUsername("player");
        UserDetalhes details = new UserDetalhes();
        details.setUser(actor);
        details.setUsername("player");

        Torneio tournament = new Torneio();
        tournament.setId(10L);
        tournament.setStatus("Aguardando Início");
        tournament.setVagasRestantes(3);
        tournament.setIsPrivado(false);
        tournament.setModeradoresIds(new ArrayList<>());

        EntrarTorneioDTO request = new EntrarTorneioDTO();
        request.setJogadorId(999L);

        when(userDetalhesRepository.findByUsername("player")).thenReturn(details);
        when(torneioRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(tournament));
        when(inscricaoRepository.existsByTorneioIdAndJogadorId(10L, 42L)).thenReturn(false);
        when(userDetalhesRepository.findByUserId(42L)).thenReturn(Optional.of(details));

        torneiosService.entrarNoTorneio(10L, request, "player");

        ArgumentCaptor<TorneioInscricao> inscricaoCaptor = ArgumentCaptor.forClass(TorneioInscricao.class);
        verify(inscricaoRepository).save(inscricaoCaptor.capture());
        assertThat(inscricaoCaptor.getValue().getJogadorId()).isEqualTo(42L);
        verify(inscricaoRepository).existsByTorneioIdAndJogadorId(10L, 42L);
        assertThat(tournament.getVagasRestantes()).isEqualTo(2);
    }

    @Test
    void deniesScoreChangesFromUsersWhoAreNotTournamentManagers() {
        User actor = new User();
        actor.setId(42L);
        UserDetalhes details = new UserDetalhes();
        details.setUser(actor);
        details.setUsername("player");

        Torneio tournament = new Torneio();
        tournament.setId(10L);
        tournament.setCriadorId(99L);
        tournament.setModeradoresIds(new ArrayList<>());

        when(torneioRepository.findById(10L)).thenReturn(Optional.of(tournament));
        when(userDetalhesRepository.findByUsername("player")).thenReturn(details);

        assertThatThrownBy(() -> torneiosService.atualizarPontuacao(10L, 15L, 100, "player"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }
}
