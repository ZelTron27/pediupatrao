package com.umc.pediupatrao.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private UsuarioRepository repo;

    private Usuario usuario(String nome, Perfil perfil) {
        Usuario usuario = new Usuario();
        usuario.setUsername(nome);
        usuario.setPerfil(perfil);
        return usuario;
    }

    @Test
    void criaUsuariosPadraoQuandoBancoVazio() throws Exception {
        when(repo.findByUsername(any())).thenReturn(Optional.empty());

        new DataInitializer().initDatabase(repo).run();

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repo, times(3)).save(captor.capture());
        List<Usuario> salvos = captor.getAllValues();
        assertEquals(Perfil.ADMIN, salvos.get(0).getPerfil());
        assertEquals(Perfil.GERENTE, salvos.get(1).getPerfil());
        assertEquals(Perfil.ATENDENTE, salvos.get(2).getPerfil());
    }

    @Test
    void naoRecriaUsuariosExistentes() throws Exception {
        when(repo.findByUsername(any())).thenAnswer(inv -> Optional.of(usuario(inv.getArgument(0), Perfil.ADMIN)));

        new DataInitializer().initDatabase(repo).run();

        verify(repo, never()).save(any());
    }

    @Test
    void corrigeAdminSemPerfil() throws Exception {
        Usuario admin = usuario("admin", null);
        when(repo.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(repo.findByUsername("gerente")).thenReturn(Optional.of(usuario("gerente", Perfil.GERENTE)));
        when(repo.findByUsername("atendente")).thenReturn(Optional.of(usuario("atendente", Perfil.ATENDENTE)));

        new DataInitializer().initDatabase(repo).run();

        assertEquals(Perfil.ADMIN, admin.getPerfil());
        verify(repo).save(admin);
    }
}
