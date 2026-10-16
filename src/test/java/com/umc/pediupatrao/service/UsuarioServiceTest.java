package com.umc.pediupatrao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    @BeforeEach
    void logarComoAdmin() {
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken("admin", "senha", "ROLE_ADMIN"));
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private Usuario usuario(String id, String username, String senha, Perfil perfil) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setUsername(username);
        usuario.setPassword(senha);
        usuario.setPerfil(perfil);
        return usuario;
    }

    @Test
    void salvarUsuarioCriptografaSenhaEZeraId() {
        when(usuarioRepository.existsByUsername("ana")).thenReturn(false);
        when(passwordEncoder.encode("123")).thenReturn("hash");

        usuarioService.salvarUsuario(usuario("lixo", "ana", "123", Perfil.ATENDENTE));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals("hash", captor.getValue().getPassword());
        assertNull(captor.getValue().getId());
    }

    @Test
    void salvarUsuarioExigeUsuarioESenha() {
        assertThrows(RegraDaCasaException.class,
                () -> usuarioService.salvarUsuario(usuario(null, " ", "123", Perfil.ATENDENTE)));
        assertThrows(RegraDaCasaException.class,
                () -> usuarioService.salvarUsuario(usuario(null, "ana", null, Perfil.ATENDENTE)));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void salvarUsuarioExigePerfil() {
        RegraDaCasaException erro = assertThrows(RegraDaCasaException.class,
                () -> usuarioService.salvarUsuario(usuario(null, "ana", "123", null)));

        assertEquals("Selecione o perfil do usuário.", erro.getMessage());
    }

    @Test
    void salvarUsuarioRejeitaNomeDuplicado() {
        when(usuarioRepository.existsByUsername("ana")).thenReturn(true);

        RegraDaCasaException erro = assertThrows(RegraDaCasaException.class,
                () -> usuarioService.salvarUsuario(usuario(null, "ana", "123", Perfil.ATENDENTE)));

        assertEquals("Já existe um usuário com esse nome.", erro.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deletarUsuarioRemovePeloId() {
        when(usuarioRepository.findById("u1")).thenReturn(Optional.of(usuario("u1", "ana", "x", Perfil.ATENDENTE)));

        usuarioService.deletarUsuario("u1");

        verify(usuarioRepository).deleteById("u1");
    }

    @Test
    void deletarUsuarioNaoPermiteExcluirASiMesmo() {
        when(usuarioRepository.findById("u1")).thenReturn(Optional.of(usuario("u1", "admin", "x", Perfil.ADMIN)));

        RegraDaCasaException erro = assertThrows(RegraDaCasaException.class,
                () -> usuarioService.deletarUsuario("u1"));

        assertEquals("Você não pode excluir o seu próprio usuário.", erro.getMessage());
        verify(usuarioRepository, never()).deleteById(any());
    }

    @Test
    void deletarUsuarioNaoPermiteRemoverUltimoAdmin() {
        when(usuarioRepository.findById("u2")).thenReturn(Optional.of(usuario("u2", "outro", "x", Perfil.ADMIN)));
        when(usuarioRepository.countByPerfil(Perfil.ADMIN)).thenReturn(1L);

        RegraDaCasaException erro = assertThrows(RegraDaCasaException.class,
                () -> usuarioService.deletarUsuario("u2"));

        assertEquals("O sistema precisa ter pelo menos um ADMIN.", erro.getMessage());
    }

    @Test
    void deletarUsuarioInexistenteLancaErro() {
        when(usuarioRepository.findById("zz")).thenReturn(Optional.empty());

        assertThrows(RegraDaCasaException.class, () -> usuarioService.deletarUsuario("zz"));
    }

    @Test
    void atualizarUsuarioMantemSenhaQuandoNaoInformada() {
        Usuario existente = usuario("u1", "ana", "hashAntigo", Perfil.ATENDENTE);
        when(usuarioRepository.findById("u1")).thenReturn(Optional.of(existente));

        usuarioService.atualizarUsuario("u1", usuario(null, "ana", "", null));

        verify(usuarioRepository).save(existente);
        assertEquals("hashAntigo", existente.getPassword());
        assertEquals(Perfil.ATENDENTE, existente.getPerfil());
    }

    @Test
    void atualizarUsuarioCriptografaNovaSenha() {
        Usuario existente = usuario("u1", "ana", "hashAntigo", Perfil.ATENDENTE);
        when(usuarioRepository.findById("u1")).thenReturn(Optional.of(existente));
        when(passwordEncoder.encode("nova")).thenReturn("hashNovo");

        usuarioService.atualizarUsuario("u1", usuario(null, "ana", "nova", null));

        assertEquals("hashNovo", existente.getPassword());
    }

    @Test
    void atualizarUsuarioRejeitaNomeJaUsadoPorOutro() {
        when(usuarioRepository.findById("u1")).thenReturn(Optional.of(usuario("u1", "ana", "x", Perfil.ATENDENTE)));
        when(usuarioRepository.existsByUsername("bia")).thenReturn(true);

        assertThrows(RegraDaCasaException.class,
                () -> usuarioService.atualizarUsuario("u1", usuario(null, "bia", null, null)));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void atualizarUsuarioNaoPermiteAlterarProprioPerfil() {
        when(usuarioRepository.findById("u1")).thenReturn(Optional.of(usuario("u1", "admin", "x", Perfil.ADMIN)));

        RegraDaCasaException erro = assertThrows(RegraDaCasaException.class,
                () -> usuarioService.atualizarUsuario("u1", usuario(null, "admin", null, Perfil.GERENTE)));

        assertEquals("Você não pode alterar o seu próprio perfil.", erro.getMessage());
    }

    @Test
    void atualizarUsuarioNaoPermiteRebaixarUltimoAdmin() {
        when(usuarioRepository.findById("u2")).thenReturn(Optional.of(usuario("u2", "outro", "x", Perfil.ADMIN)));
        when(usuarioRepository.countByPerfil(Perfil.ADMIN)).thenReturn(1L);

        assertThrows(RegraDaCasaException.class,
                () -> usuarioService.atualizarUsuario("u2", usuario(null, "outro", null, Perfil.GERENTE)));
    }
}
