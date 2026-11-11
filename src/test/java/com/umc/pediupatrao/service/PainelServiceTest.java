package com.umc.pediupatrao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.dto.FiltroPainel;
import com.umc.pediupatrao.dto.IndicadoresVenda;
import com.umc.pediupatrao.dto.ProdutoNoRanking;
import com.umc.pediupatrao.dto.VendaDoDia;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.repository.PedidoRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Range;

@ExtendWith(MockitoExtension.class)
class PainelServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @InjectMocks
    private PainelService painelService;

    private Pedido pedido(StatusPedido status, Double total, Double desconto) {
        Pedido pedido = new Pedido();
        pedido.setStatus(status);
        pedido.setValorTotal(total);
        pedido.setValorDesconto(desconto);
        return pedido;
    }

    @Test
    void indicadoresIgnoramPedidosCanceladosNoFaturamento() {
        when(pedidoRepository.findAll()).thenReturn(List.of(
                pedido(StatusPedido.FINALIZADO, 90.0, 10.0),
                pedido(StatusPedido.RECEBIDO, 45.0, 0.0),
                pedido(StatusPedido.CANCELADO, 200.0, 0.0)));

        IndicadoresVenda indicadores = painelService.indicadores(FiltroPainel.NENHUM);

        assertEquals(135.0, indicadores.faturamento());
        assertEquals(2, indicadores.pedidosFaturados());
        assertEquals(67.5, indicadores.ticketMedio());
        assertEquals(1, indicadores.cancelados());
        assertEquals(10.0, indicadores.somaDescontos());
    }

    @Test
    void vendasSaoSomadasPorDiaEmOrdemCronologica() {
        Pedido segundaManha = pedido(StatusPedido.FINALIZADO, 40.0, 0.0);
        segundaManha.setDataHoraEntrada(LocalDateTime.of(2026, 11, 2, 11, 0));
        Pedido segundaNoite = pedido(StatusPedido.PRONTO, 60.0, 0.0);
        segundaNoite.setDataHoraEntrada(LocalDateTime.of(2026, 11, 2, 21, 30));
        Pedido domingo = pedido(StatusPedido.RECEBIDO, 25.5, 0.0);
        domingo.setDataHoraEntrada(LocalDateTime.of(2026, 11, 1, 19, 0));
        Pedido cancelado = pedido(StatusPedido.CANCELADO, 100.0, 0.0);
        cancelado.setDataHoraEntrada(LocalDateTime.of(2026, 11, 2, 20, 0));
        when(pedidoRepository.findAll()).thenReturn(List.of(segundaManha, cancelado, domingo, segundaNoite));

        List<VendaDoDia> vendas = painelService.vendasPorDia(FiltroPainel.NENHUM);

        assertEquals(2, vendas.size());
        assertEquals(LocalDate.of(2026, 11, 1), vendas.get(0).dia());
        assertEquals(25.5, vendas.get(0).faturamento());
        assertEquals(LocalDate.of(2026, 11, 2), vendas.get(1).dia());
        assertEquals(2, vendas.get(1).pedidos());
        assertEquals(100.0, vendas.get(1).faturamento());
    }

    @Test
    void contagemPorStatusTrazTodosOsStatusMesmoSemPedido() {
        when(pedidoRepository.findAll()).thenReturn(List.of(
                pedido(StatusPedido.RECEBIDO, 10.0, 0.0),
                pedido(StatusPedido.RECEBIDO, 20.0, 0.0),
                pedido(StatusPedido.CANCELADO, 30.0, 0.0),
                pedido(null, 5.0, 0.0)));

        Map<StatusPedido, Long> contagem = painelService.pedidosPorStatus(FiltroPainel.NENHUM);

        assertEquals(StatusPedido.values().length, contagem.size());
        assertEquals(2L, contagem.get(StatusPedido.RECEBIDO));
        assertEquals(1L, contagem.get(StatusPedido.CANCELADO));
        assertEquals(0L, contagem.get(StatusPedido.FINALIZADO));
    }

    private ItemPedido item(String produtoId, String nome, int quantidade, double subtotal) {
        ItemPedido item = new ItemPedido();
        item.setProdutoId(produtoId);
        item.setNomeProduto(nome);
        item.setQuantidade(quantidade);
        item.setSubtotal(subtotal);
        return item;
    }

    @Test
    void rankingSomaUnidadesDosPedidosValidosEOrdenaDoMaiorParaOMenor() {
        Pedido primeiro = pedido(StatusPedido.FINALIZADO, 0.0, 0.0);
        primeiro.setItens(List.of(item("pz1", "Pizza 10 Queijos", 1, 45.0), item("bb1", "Fanta Guaraná", 2, 14.0)));
        Pedido segundo = pedido(StatusPedido.RECEBIDO, 0.0, 0.0);
        segundo.setItens(List.of(item("bb1", "Fanta Guaraná", 1, 7.0), item("pz2", "Pizza Portuguesa", 1, 40.0)));
        Pedido cancelado = pedido(StatusPedido.CANCELADO, 0.0, 0.0);
        cancelado.setItens(List.of(item("pz2", "Pizza Portuguesa", 5, 200.0)));
        when(pedidoRepository.findAll()).thenReturn(List.of(primeiro, segundo, cancelado));

        List<ProdutoNoRanking> ranking = painelService.maisVendidos(FiltroPainel.NENHUM, 2);

        assertEquals(2, ranking.size());
        assertEquals("bb1", ranking.get(0).produtoId());
        assertEquals(3, ranking.get(0).unidades());
        assertEquals(21.0, ranking.get(0).receita());
        assertEquals("pz1", ranking.get(1).produtoId());
    }

    @Test
    void rankingNaoAceitaLimiteZero() {
        RegraDaCasaException erro = assertThrows(RegraDaCasaException.class, () -> painelService.maisVendidos(FiltroPainel.NENHUM, 0));
        assertEquals("O ranking precisa de pelo menos 1 posição.", erro.getMessage());
    }

    @Test
    void semPedidosOsIndicadoresFicamZerados() {
        when(pedidoRepository.findAll()).thenReturn(List.of());

        IndicadoresVenda indicadores = painelService.indicadores(FiltroPainel.NENHUM);

        assertEquals(0.0, indicadores.faturamento());
        assertEquals(0, indicadores.pedidosFaturados());
        assertEquals(0.0, indicadores.ticketMedio());
        assertEquals(0, indicadores.cancelados());
    }

    @Test
    void pedidoAntigoSemValoresContaComoZero() {
        when(pedidoRepository.findAll()).thenReturn(List.of(
                pedido(StatusPedido.RECEBIDO, null, null),
                pedido(StatusPedido.PRONTO, 50.0, null)));

        IndicadoresVenda indicadores = painelService.indicadores(FiltroPainel.NENHUM);

        assertEquals(50.0, indicadores.faturamento());
        assertEquals(25.0, indicadores.ticketMedio());
        assertEquals(0.0, indicadores.somaDescontos());
    }

    @Test
    void periodoConsultaOBancoDoInicioDoPrimeiroDiaAteOFimDoUltimo() {
        when(pedidoRepository.findByDataHoraEntradaBetween(
                Range.rightOpen(LocalDateTime.of(2026, 11, 1, 0, 0), LocalDateTime.of(2026, 11, 8, 0, 0))))
                .thenReturn(List.of(pedido(StatusPedido.FINALIZADO, 80.0, 0.0)));

        IndicadoresVenda indicadores = painelService.indicadores(
                new FiltroPainel(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 7), null));

        assertEquals(80.0, indicadores.faturamento());
        verify(pedidoRepository, never()).findAll();
    }

    @Test
    void filtroDeStatusMantemSomenteOsPedidosDaqueleStatus() {
        when(pedidoRepository.findAll()).thenReturn(List.of(
                pedido(StatusPedido.FINALIZADO, 80.0, 0.0),
                pedido(StatusPedido.RECEBIDO, 30.0, 0.0)));

        IndicadoresVenda indicadores = painelService.indicadores(new FiltroPainel(null, null, StatusPedido.FINALIZADO));

        assertEquals(1, indicadores.pedidosFaturados());
        assertEquals(80.0, indicadores.faturamento());
    }

    @Test
    void dataInicialDepoisDaFinalEhRecusada() {
        FiltroPainel invertido = new FiltroPainel(LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 1), null);

        RegraDaCasaException erro = assertThrows(RegraDaCasaException.class, () -> painelService.indicadores(invertido));

        assertEquals("O período informado termina antes de começar.", erro.getMessage());
        verify(pedidoRepository, never()).findByDataHoraEntradaBetween(any());
    }

    @Test
    void movimentoContaPedidosPorDiaDaSemanaEHoraDeEntrada() {
        Pedido sextaNoite = pedido(StatusPedido.FINALIZADO, 50.0, 0.0);
        sextaNoite.setDataHoraEntrada(LocalDateTime.of(2026, 11, 6, 20, 15));
        Pedido outraSextaNoite = pedido(StatusPedido.CANCELADO, 30.0, 0.0);
        outraSextaNoite.setDataHoraEntrada(LocalDateTime.of(2026, 10, 30, 20, 50));
        Pedido sabadoAlmoco = pedido(StatusPedido.RECEBIDO, 40.0, 0.0);
        sabadoAlmoco.setDataHoraEntrada(LocalDateTime.of(2026, 11, 7, 12, 0));
        when(pedidoRepository.findAll()).thenReturn(List.of(sextaNoite, outraSextaNoite, sabadoAlmoco,
                pedido(StatusPedido.RECEBIDO, 10.0, 0.0)));

        Map<DayOfWeek, long[]> movimento = painelService.movimentoPorHorario(FiltroPainel.NENHUM);

        assertEquals(7, movimento.size());
        assertEquals(2, movimento.get(DayOfWeek.FRIDAY)[20]);
        assertEquals(1, movimento.get(DayOfWeek.SATURDAY)[12]);
        assertEquals(0, movimento.get(DayOfWeek.MONDAY)[20]);
    }
}
