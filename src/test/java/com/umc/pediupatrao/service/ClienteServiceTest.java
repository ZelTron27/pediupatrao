package com.umc.pediupatrao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.EventoAuditoria.CampoAlterado;
import com.umc.pediupatrao.entity.TipoEvento;
import com.umc.pediupatrao.repository.ClienteRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private TrilhaAuditoriaService trilhaAuditoriaService;

    @InjectMocks
    private ClienteService clienteService;

    private Cliente cliente(String id, String nome, String telefone) {
        Cliente cliente = new Cliente();
        cliente.setId(id);
        cliente.setNome(nome);
        cliente.setTelefone(telefone);
        return cliente;
    }

    @Test
    void salvarClienteNovoRegistraInclusaoComCamposPreenchidos() {
        Cliente novo = cliente(null, "Ana", "1199999");
        when(clienteRepository.save(novo)).thenAnswer(inv -> {
            novo.setId("c1");
            return novo;
        });

        Cliente salvo = clienteService.salvar(novo);

        assertSame(novo, salvo);
        ArgumentCaptor<List<CampoAlterado>> campos = ArgumentCaptor.forClass(List.class);
        verify(trilhaAuditoriaService).registrar(eq(TipoEvento.INCLUSAO_CLIENTE), eq("Cliente"), eq("c1"),
                isNull(), campos.capture());
        assertEquals(2, campos.getValue().size());
        assertEquals("nome", campos.getValue().get(0).campo());
    }

    @Test
    void salvarClienteExistenteRegistraApenasCamposAlterados() {
        when(clienteRepository.findById("c1")).thenReturn(Optional.of(cliente("c1", "Ana", "1199999")));
        Cliente editado = cliente("c1", "Ana Maria", "1199999");
        when(clienteRepository.save(editado)).thenReturn(editado);

        clienteService.salvar(editado);

        ArgumentCaptor<List<CampoAlterado>> campos = ArgumentCaptor.forClass(List.class);
        verify(trilhaAuditoriaService).registrar(eq(TipoEvento.ALTERACAO_CLIENTE), eq("Cliente"), eq("c1"),
                isNull(), campos.capture());
        assertEquals(1, campos.getValue().size());
        assertEquals("Ana", campos.getValue().get(0).anterior());
        assertEquals("Ana Maria", campos.getValue().get(0).novo());
    }

    @Test
    void salvarClienteSemMudancaNaoRegistraAuditoria() {
        when(clienteRepository.findById("c1")).thenReturn(Optional.of(cliente("c1", "Ana", " ")));
        Cliente igual = cliente("c1", " Ana ", null);
        when(clienteRepository.save(igual)).thenReturn(igual);

        clienteService.salvar(igual);

        verify(trilhaAuditoriaService, never()).registrar(any(), any(), any(), any(), anyList());
    }

    @Test
    void salvarClienteInexistenteLancaErro() {
        when(clienteRepository.findById("x")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> clienteService.salvar(cliente("x", "Ana", null)));
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void novoClienteListarBuscarEExcluirDelegamAoRepositorio() {
        Cliente c = cliente("c1", "Ana", null);
        when(clienteRepository.save(c)).thenReturn(c);
        when(clienteRepository.findAll()).thenReturn(List.of(c));
        when(clienteRepository.findById("c1")).thenReturn(Optional.of(c));

        assertSame(c, clienteService.novoCliente(c));
        assertEquals(1, clienteService.listarClientes().size());
        assertTrue(clienteService.buscarPorId("c1").isPresent());
        clienteService.excluir("c1");
        verify(clienteRepository).deleteById("c1");
    }
}
