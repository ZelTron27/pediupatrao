package com.umc.pediupatrao.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.ProdutoRepository;
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

    @Mock
    private ProdutoRepository produtoRepo;

    private Usuario usuario(String nome, Perfil perfil) {
        Usuario usuario = new Usuario();
        usuario.setUsername(nome);
        usuario.setPerfil(perfil);
        return usuario;
    }

    @Test
    void criaUsuariosPadraoQuandoBancoVazio() throws Exception {
        when(repo.findByUsername(any())).thenReturn(Optional.empty());

        new DataInitializer().initDatabase(repo, produtoRepo).run();

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

        new DataInitializer().initDatabase(repo, produtoRepo).run();

        verify(repo, never()).save(any());
    }

    @Test
    void corrigeAdminSemPerfil() throws Exception {
        Usuario admin = usuario("admin", null);
        when(repo.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(repo.findByUsername("gerente")).thenReturn(Optional.of(usuario("gerente", Perfil.GERENTE)));
        when(repo.findByUsername("atendente")).thenReturn(Optional.of(usuario("atendente", Perfil.ATENDENTE)));

        new DataInitializer().initDatabase(repo, produtoRepo).run();

        assertEquals(Perfil.ADMIN, admin.getPerfil());
        verify(repo).save(admin);
    }

    @Test
    void criaProdutosIniciaisQuandoNaoHaNenhum() throws Exception {
        when(repo.findByUsername(any())).thenReturn(Optional.of(usuario("x", Perfil.ADMIN)));
        when(produtoRepo.count()).thenReturn(0L);

        new DataInitializer().initDatabase(repo, produtoRepo).run();

        ArgumentCaptor<List<Produto>> captor = ArgumentCaptor.forClass(List.class);
        verify(produtoRepo).saveAll(captor.capture());
        List<Produto> produtos = captor.getValue();
        assertEquals(3, produtos.size());
        assertEquals(40.0, produtos.get(0).getPreco());
        assertEquals(45.0, produtos.get(1).getPreco());
        assertEquals(7.0, produtos.get(2).getPreco());
        assertTrue(produtos.stream().allMatch(Produto::getAtivo));
    }

    @Test
    void naoDuplicaProdutosQuandoJaExistem() throws Exception {
        when(repo.findByUsername(any())).thenReturn(Optional.of(usuario("x", Perfil.ADMIN)));
        when(produtoRepo.count()).thenReturn(3L);

        new DataInitializer().initDatabase(repo, produtoRepo).run();

        verify(produtoRepo, never()).saveAll(any());
    }
}
