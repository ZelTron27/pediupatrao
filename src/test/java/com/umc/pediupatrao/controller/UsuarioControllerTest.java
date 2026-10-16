package com.umc.pediupatrao.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.service.UsuarioService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private UsuarioController controller;

    @Test
    void listarTodosRetornaOk() {
        when(usuarioService.listarTodos()).thenReturn(List.of(new Usuario()));

        assertEquals(HttpStatus.OK, controller.listarTodos().getStatusCode());
    }

    @Test
    void buscarPorIdRetornaOkOuNotFound() {
        when(usuarioService.buscarPorId("u1")).thenReturn(Optional.of(new Usuario()));
        when(usuarioService.buscarPorId("u2")).thenReturn(Optional.empty());

        assertEquals(HttpStatus.OK, controller.buscarPorId("u1").getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controller.buscarPorId("u2").getStatusCode());
    }

    @Test
    void criarRetornaCreated() {
        Usuario u = new Usuario();

        assertEquals(HttpStatus.CREATED, controller.criarUsuario(u).getStatusCode());
        verify(usuarioService).salvarUsuario(u);
    }

    @Test
    void atualizarExistenteRetornaUsuarioAtualizado() {
        Usuario atualizado = new Usuario();
        when(usuarioService.buscarPorId("u1")).thenReturn(Optional.of(atualizado));

        var resposta = controller.atualizarUsuario("u1", atualizado);

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        verify(usuarioService).atualizarUsuario("u1", atualizado);
    }

    @Test
    void atualizarInexistenteRetornaNotFound() {
        when(usuarioService.buscarPorId("u2")).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND, controller.atualizarUsuario("u2", new Usuario()).getStatusCode());
        verify(usuarioService, never()).atualizarUsuario(anyString(), any());
    }

    @Test
    void deletarExistenteRetornaNoContent() {
        when(usuarioService.buscarPorId("u1")).thenReturn(Optional.of(new Usuario()));

        assertEquals(HttpStatus.NO_CONTENT, controller.deletarUsuario("u1").getStatusCode());
        verify(usuarioService).deletarUsuario("u1");
    }

    @Test
    void deletarInexistenteRetornaNotFound() {
        when(usuarioService.buscarPorId("u2")).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND, controller.deletarUsuario("u2").getStatusCode());
        verify(usuarioService, never()).deletarUsuario(anyString());
    }
}
