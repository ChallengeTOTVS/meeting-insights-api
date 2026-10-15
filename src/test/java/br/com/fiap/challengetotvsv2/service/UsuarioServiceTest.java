package br.com.fiap.challengetotvsv2.service;

import br.com.fiap.challengetotvsv2.dto.usuario.UsuarioRequestDto;
import br.com.fiap.challengetotvsv2.enums.Role;
import br.com.fiap.challengetotvsv2.model.UsuarioEntity;
import br.com.fiap.challengetotvsv2.repository.IUsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UsuarioServiceTest {
    private final IUsuarioRepository repo = mock(IUsuarioRepository.class);
    private final UsuarioService service = new UsuarioService(repo, new BCryptPasswordEncoder());

    @AfterEach
    void limpar() { SecurityContextHolder.clearContext(); }

    @Test
    void adminCriaUsuarioDoPerfilConsulta() {
        when(repo.existsByEmail("mkt@empresa.com")).thenReturn(false);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var criado = service.cadastrarUsuario(new UsuarioRequestDto("Marketing", " mkt@empresa.com ", "senhaForte1", Role.CONSULTA));
        assertThat(criado.role()).isEqualTo(Role.CONSULTA);
        assertThat(criado.email()).isEqualTo("mkt@empresa.com");
    }

    @Test
    void semPerfilInformadoViraVendedor() {
        when(repo.existsByEmail(any())).thenReturn(false);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThat(service.cadastrarUsuario(new UsuarioRequestDto(null, "v@empresa.com", "senhaForte1", null)).role())
                .isEqualTo(Role.FUNCIONARIO);
    }

    @Test
    void adminNaoPodeTirarOProprioAcesso() {
        UsuarioEntity admin = UsuarioEntity.builder().id(UUID.randomUUID()).email("admin@empresa.com").role(Role.ADMIN).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@empresa.com", null, List.of()));
        when(repo.findById(admin.getId())).thenReturn(Optional.of(admin));
        assertThatThrownBy(() -> service.alterarPerfil(admin.getId(), Role.CONSULTA))
                .isInstanceOf(ResponseStatusException.class);
        verify(repo, never()).save(any());
    }
}
