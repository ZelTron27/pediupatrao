package com.umc.pediupatrao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.dto.DescontoPedido;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoEvento;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private TrilhaAuditoriaService trilhaAuditoriaService;

    @InjectMocks
    private PedidoService pedidoService;

    @BeforeEach
    void logarComoGerente() {
        logar("maria", "ROLE_GERENTE");
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private void logar(String nome, String papel) {
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(nome, "senha", papel));
    }

    private Produto produto(String id, String nome, Double preco, Boolean ativo) {
        Produto produto = new Produto();
        produto.setId(id);
        produto.setNome(nome);
        produto.setPreco(preco);
        produto.setAtivo(ativo);
        return produto;
    }

    private ItemPedido itemEnviado(String produtoId, int quantidade) {
        ItemPedido item = new ItemPedido();
        item.setProdutoId(produtoId);
        item.setQuantidade(quantidade);
        return item;
    }

    private Pedido pedidoNovo(String clienteId, ItemPedido... itens) {
        Pedido pedido = new Pedido();
        pedido.setClienteId(clienteId);
        pedido.setItens(new ArrayList<>(List.of(itens)));
        return pedido;
    }

    private Pedido pedidoExistente(StatusPedido status, double subtotal) {
        Pedido pedido = new Pedido();
        pedido.setId("p1");
        pedido.setStatus(status);
        pedido.setSubtotal(subtotal);
        pedido.setPercentualDesconto(0.0);
        pedido.setValorDesconto(0.0);
        pedido.setValorTotal(subtotal);
        return pedido;
    }

    @Test
    void criarPedidoCalculaTotaisEPreencheCamposIniciais() {
        when(clienteRepository.existsById("c1")).thenReturn(true);
        when(produtoRepository.findById("pr1")).thenReturn(Optional.of(produto("pr1", "Calabresa", 40.0, true)));
        when(produtoRepository.findById("pr2")).thenReturn(Optional.of(produto("pr2", "Guaraná", 8.5, true)));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            p.setId("novo");
            return p;
        });

        Pedido salvo = pedidoService.criarPedido(pedidoNovo("c1", itemEnviado("pr1", 2), itemEnviado("pr2", 3)));

        assertEquals(105.5, salvo.getSubtotal());
        assertEquals(0.0, salvo.getValorDesconto());
        assertEquals(105.5, salvo.getValorTotal());
        assertEquals(StatusPedido.RECEBIDO, salvo.getStatus());
        assertEquals("maria", salvo.getResponsavel());
        assertNotNull(salvo.getDataHoraEntrada());
        assertEquals(2, salvo.getItens().size());
        assertEquals("Calabresa", salvo.getItens().get(0).getNomeProduto());
        verify(trilhaAuditoriaService).registrar(eq(TipoEvento.INCLUSAO_PEDIDO), eq("Pedido"), eq("novo"), isNull(), anyList());
    }

    @Test
    void criarPedidoRejeitaClienteInexistente() {
        when(clienteRepository.existsById("c1")).thenReturn(false);

        RegraDaCasaException erro = assertThrows(RegraDaCasaException.class,
                () -> pedidoService.criarPedido(pedidoNovo("c1", itemEnviado("pr1", 1))));

        assertEquals("Cliente não encontrado.", erro.getMessage());
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void criarPedidoRejeitaPedidoSemClienteInformado() {
        assertThrows(RegraDaCasaException.class,
                () -> pedidoService.criarPedido(pedidoNovo(null, itemEnviado("pr1", 1))));
    }

    @Test
    void criarPedidoRejeitaPedidoSemItens() {
        when(clienteRepository.existsById("c1")).thenReturn(true);

        RegraDaCasaException erro = assertThrows(RegraDaCasaException.class,
                () -> pedidoService.criarPedido(pedidoNovo("c1")));

        assertEquals("O pedido precisa ter ao menos um item.", erro.getMessage());
    }

    @Test
    void criarPedidoRejeitaQuantidadeZero() {
        when(clienteRepository.existsById("c1")).thenReturn(true);

        RegraDaCasaException erro = assertThrows(RegraDaCasaException.class,
                () -> pedidoService.criarPedido(pedidoNovo("c1", itemEnviado("pr1", 0))));

        assertEquals("A quantidade de cada item deve ser maior que zero.", erro.getMessage());
    }

    @Test
    void criarPedidoRejeitaProdutoInexistente() {
        when(clienteRepository.existsById("c1")).thenReturn(true);
        when(produtoRepository.findById("pr1")).thenReturn(Optional.empty());

        RegraDaCasaException erro = assertThrows(RegraDaCasaException.class,
                () -> pedidoService.criarPedido(pedidoNovo("c1", itemEnviado("pr1", 1))));

        assertEquals("Produto não encontrado.", erro.getMessage());
    }

    @Test
    void criarPedidoRejeitaProdutoInativo() {
        when(clienteRepository.existsById("c1")).thenReturn(true);
        when(produtoRepository.findById("pr1")).thenReturn(Optional.of(produto("pr1", "Calabresa", 40.0, false)));

        RegraDaCasaException erro = assertThrows(RegraDaCasaException.class,
                () -> pedidoService.criarPedido(pedidoNovo("c1", itemEnviado("pr1", 1))));

        assertEquals("O produto Calabresa está inativo.", erro.getMessage());
    }

    @Test
    void criarPedidoRejeitaProdutoSemPrecoValido() {
        when(clienteRepository.existsById("c1")).thenReturn(true);
        when(produtoRepository.findById("pr1")).thenReturn(Optional.of(produto("pr1", "Calabresa", 0.0, true)));

        RegraDaCasaException erro = assertThrows(RegraDaCasaException.class,
                () -> pedidoService.criarPedido(pedidoNovo("c1", itemEnviado("pr1", 1))));

        assertEquals("O produto Calabresa está sem preço válido.", erro.getMessage());
    }

    @Test
    void atualizarStatusAvancaFluxoValido() {
        Pedido pedido = pedidoExistente(StatusPedido.RECEBIDO, 50.0);
        when(pedidoRepository.findById("p1")).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(pedido)).thenReturn(pedido);

        Pedido salvo = pedidoService.atualizarStatus("p1", StatusPedido.EM_PREPARACAO, null);

        assertEquals(StatusPedido.EM_PREPARACAO, salvo.getStatus());
        verify(trilhaAuditoriaService).registrar(eq(TipoEvento.MUDANCA_STATUS), eq("Pedido"), eq("p1"), isNull(), anyList());
    }

    @Test
    void atualizarStatusRejeitaTransicaoInvalida() {
        when(pedidoRepository.findById("p1")).thenReturn(Optional.of(pedidoExistente(StatusPedido.RECEBIDO, 50.0)));

        assertThrows(RegraDaCasaException.class,
                () -> pedidoService.atualizarStatus("p1", StatusPedido.FINALIZADO, null));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void atualizarStatusRejeitaPedidoCancelado() {
        when(pedidoRepository.findById("p1")).thenReturn(Optional.of(pedidoExistente(StatusPedido.CANCELADO, 50.0)));

        RegraDaCasaException erro = assertThrows(RegraDaCasaException.class,
                () -> pedidoService.atualizarStatus("p1", StatusPedido.RECEBIDO, null));

        assertEquals("Pedido cancelado não pode mudar de status.", erro.getMessage());
    }

    @Test
    void atualizarStatusRejeitaPedidoInexistente() {
        when(pedidoRepository.findById("x")).thenReturn(Optional.empty());

        RegraDaCasaException erro = assertThrows(RegraDaCasaException.class,
                () -> pedidoService.atualizarStatus("x", StatusPedido.EM_PREPARACAO, null));

        assertEquals("Pedido não encontrado.", erro.getMessage());
    }

    @Test
    void cancelarExigeJustificativa() {
        when(pedidoRepository.findById("p1")).thenReturn(Optional.of(pedidoExistente(StatusPedido.PRONTO, 50.0)));

        RegraDaCasaException erro = assertThrows(RegraDaCasaException.class,
                () -> pedidoService.atualizarStatus("p1", StatusPedido.CANCELADO, "  "));

        assertEquals("Informe a justificativa do cancelamento.", erro.getMessage());
    }

    @Test
    void cancelarComJustificativaRegistraCancelamento() {
        Pedido pedido = pedidoExistente(StatusPedido.PRONTO, 50.0);
        when(pedidoRepository.findById("p1")).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(pedido)).thenReturn(pedido);

        Pedido salvo = pedidoService.atualizarStatus("p1", StatusPedido.CANCELADO, " cliente desistiu ");

        assertEquals(StatusPedido.CANCELADO, salvo.getStatus());
        assertEquals("cliente desistiu", salvo.getJustificativaCancelamento());
        verify(trilhaAuditoriaService).registrar(eq(TipoEvento.CANCELAMENTO), eq("Pedido"), eq("p1"),
                eq("cliente desistiu"), anyList());
    }

    @Test
    void justificativaForaDoCancelamentoERejeitada() {
        when(pedidoRepository.findById("p1")).thenReturn(Optional.of(pedidoExistente(StatusPedido.RECEBIDO, 50.0)));

        assertThrows(RegraDaCasaException.class,
                () -> pedidoService.atualizarStatus("p1", StatusPedido.EM_PREPARACAO, "motivo qualquer"));
    }

    @Test
    void saidaDoPedidoRegistraResponsavelEHorario() {
        Pedido pedido = pedidoExistente(StatusPedido.PRONTO, 50.0);
        when(pedidoRepository.findById("p1")).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(pedido)).thenReturn(pedido);

        Pedido salvo = pedidoService.atualizarStatus("p1", StatusPedido.SAIU_PARA_ENTREGA, null);

        assertEquals("maria", salvo.getResponsavelSaida());
        assertNotNull(salvo.getDataHoraSaida());
    }

    @Test
    void aplicarDescontoRecalculaValores() {
        Pedido pedido = pedidoExistente(StatusPedido.RECEBIDO, 200.0);
        when(pedidoRepository.findById("p1")).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(pedido)).thenReturn(pedido);

        Pedido salvo = pedidoService.aplicarDesconto("p1", new DescontoPedido(15.0));

        assertEquals(15.0, salvo.getPercentualDesconto());
        assertEquals(30.0, salvo.getValorDesconto());
        assertEquals(170.0, salvo.getValorTotal());
        verify(trilhaAuditoriaService).registrar(eq(TipoEvento.DESCONTO), eq("Pedido"), eq("p1"), isNull(), anyList());
    }

    @Test
    void aplicarDescontoAcimaDoLimiteDoPerfilERejeitado() {
        when(pedidoRepository.findById("p1")).thenReturn(Optional.of(pedidoExistente(StatusPedido.RECEBIDO, 200.0)));

        RegraDaCasaException erro = assertThrows(RegraDaCasaException.class,
                () -> pedidoService.aplicarDesconto("p1", new DescontoPedido(25.0)));

        assertEquals("O desconto deve ficar entre 0% e 20%.", erro.getMessage());
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void aplicarDescontoRejeitaPercentualNegativoOuNulo() {
        when(pedidoRepository.findById("p1")).thenReturn(Optional.of(pedidoExistente(StatusPedido.RECEBIDO, 200.0)));

        assertThrows(RegraDaCasaException.class,
                () -> pedidoService.aplicarDesconto("p1", new DescontoPedido(-1.0)));
        assertThrows(RegraDaCasaException.class,
                () -> pedidoService.aplicarDesconto("p1", new DescontoPedido(null)));
    }

    @Test
    void aplicarDescontoRejeitaPedidoJaEncerrado() {
        when(pedidoRepository.findById("p1")).thenReturn(Optional.of(pedidoExistente(StatusPedido.FINALIZADO, 200.0)));

        assertThrows(RegraDaCasaException.class,
                () -> pedidoService.aplicarDesconto("p1", new DescontoPedido(10.0)));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void limiteDescontoDependeDoPerfilLogado() {
        assertEquals(20, pedidoService.limiteDesconto());

        logar("joao", "ROLE_ATENDENTE");
        assertEquals(0, pedidoService.limiteDesconto());
    }
}
