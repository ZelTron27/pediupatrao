package com.umc.pediupatrao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.entity.EventoAuditoria;
import com.umc.pediupatrao.entity.EventoAuditoria.CampoAlterado;
import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.TipoEvento;
import com.umc.pediupatrao.repository.EventoAuditoriaRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class TrilhaAuditoriaServiceTest {

    @Mock
    private EventoAuditoriaRepository eventoAuditoriaRepository;

    @InjectMocks
    private TrilhaAuditoriaService trilhaAuditoriaService;

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registrarGravaEventoComUsuarioEPerfilLogados() {
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken("maria", "s", "ROLE_GERENTE"));
        List<CampoAlterado> campos = List.of(CampoAlterado.de("status", "A", "B"));

        trilhaAuditoriaService.registrar(TipoEvento.MUDANCA_STATUS, "Pedido", "p1", "motivo", campos);

        ArgumentCaptor<EventoAuditoria> captor = ArgumentCaptor.forClass(EventoAuditoria.class);
        verify(eventoAuditoriaRepository).save(captor.capture());
        EventoAuditoria evento = captor.getValue();
        assertEquals("maria", evento.getUsuario());
        assertEquals(Perfil.GERENTE, evento.getPerfil());
        assertEquals(TipoEvento.MUDANCA_STATUS, evento.getTipo());
        assertEquals("Pedido", evento.getEntidade());
        assertEquals("p1", evento.getEntidadeId());
        assertEquals("motivo", evento.getJustificativa());
        assertEquals(campos, evento.getCampos());
        assertNotNull(evento.getDataHora());
    }

    @Test
    void registrarSemAutoridadesGravaPerfilNulo() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("x", "s"));

        trilhaAuditoriaService.registrar(TipoEvento.DESCONTO, "Pedido", "p1", null, List.of());

        ArgumentCaptor<EventoAuditoria> captor = ArgumentCaptor.forClass(EventoAuditoria.class);
        verify(eventoAuditoriaRepository).save(captor.capture());
        assertNull(captor.getValue().getPerfil());
    }

    @Test
    void consultarRetornaEventosEmOrdemDecrescente() {
        List<EventoAuditoria> eventos = List.of(new EventoAuditoria());
        when(eventoAuditoriaRepository.findAllByOrderByDataHoraDesc()).thenReturn(eventos);

        assertEquals(eventos, trilhaAuditoriaService.consultar());
    }

    @Test
    void campoAlteradoConverteValoresParaTextoPreservandoNulos() {
        CampoAlterado campo = CampoAlterado.de("valor", null, 10.5);

        assertNull(campo.anterior());
        assertEquals("10.5", campo.novo());
    }
}
