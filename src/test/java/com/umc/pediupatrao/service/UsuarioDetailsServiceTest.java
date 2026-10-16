package com.umc.pediupatrao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class UsuarioDetailsServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioDetailsService service;

    private Usuario usuario(Perfil perfil) {
        Usuario usuario = new Usuario();
        usuario.setUsername("ana");
        usuario.setPassword("hash");
        usuario.setPerfil(perfil);
        return usuario;
    }

    @Test
    void carregaUsuarioComPapelDoPerfil() {
        when(usuarioRepository.findByUsername("ana")).thenReturn(Optional.of(usuario(Perfil.GERENTE)));

        UserDetails detalhes = service.loadUserByUsername("ana");

        assertEquals("ana", detalhes.getUsername());
        assertEquals("hash", detalhes.getPassword());
        assertTrue(detalhes.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_GERENTE")));
    }

    @Test
    void usuarioInexistenteLancaExcecao() {
        when(usuarioRepository.findByUsername("zz")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("zz"));
    }

    @Test
    void usuarioSemPerfilLancaExcecao() {
        when(usuarioRepository.findByUsername("ana")).thenReturn(Optional.of(usuario(null)));

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("ana"));
    }
}
