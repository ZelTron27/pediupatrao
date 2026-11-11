package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.FiltroPainel;
import com.umc.pediupatrao.dto.IndicadoresVenda;
import com.umc.pediupatrao.dto.ProdutoNoRanking;
import com.umc.pediupatrao.dto.VendaDoDia;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.repository.PedidoRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Range;
import org.springframework.stereotype.Service;

@Service
public class PainelService {

    @Autowired
    private PedidoRepository pedidoRepository;

    public IndicadoresVenda indicadores(FiltroPainel filtro) {
        Map<Boolean, List<Pedido>> separados = pedidosDo(filtro).stream()
                .collect(Collectors.partitioningBy(p -> p.getStatus() == StatusPedido.CANCELADO));

        List<Pedido> validos = separados.get(false);
        DoubleSummaryStatistics totais = validos.stream()
                .mapToDouble(p -> Objects.requireNonNullElse(p.getValorTotal(), 0.0))
                .summaryStatistics();
        double descontos = validos.stream()
                .mapToDouble(p -> Objects.requireNonNullElse(p.getValorDesconto(), 0.0))
                .sum();

        return new IndicadoresVenda(centavos(totais.getSum()), totais.getCount(),
                centavos(totais.getAverage()), separados.get(true).size(), centavos(descontos));
    }

    public List<VendaDoDia> vendasPorDia(FiltroPainel filtro) {
        Map<LocalDate, DoubleSummaryStatistics> porDia = pedidosDo(filtro).stream()
                .filter(p -> p.getStatus() != StatusPedido.CANCELADO && p.getDataHoraEntrada() != null)
                .collect(Collectors.groupingBy(p -> p.getDataHoraEntrada().toLocalDate(), TreeMap::new,
                        Collectors.summarizingDouble(p -> Objects.requireNonNullElse(p.getValorTotal(), 0.0))));

        return porDia.entrySet().stream()
                .map(dia -> new VendaDoDia(dia.getKey(), dia.getValue().getCount(), centavos(dia.getValue().getSum())))
                .toList();
    }

    public Map<StatusPedido, Long> pedidosPorStatus(FiltroPainel filtro) {
        Map<StatusPedido, Long> contagem = new EnumMap<>(StatusPedido.class);
        for (StatusPedido status : StatusPedido.values()) {
            contagem.put(status, 0L);
        }
        pedidosDo(filtro).stream()
                .map(Pedido::getStatus)
                .filter(Objects::nonNull)
                .forEach(status -> contagem.merge(status, 1L, Long::sum));
        return contagem;
    }

    public List<ProdutoNoRanking> maisVendidos(FiltroPainel filtro, int limite) {
        if (limite < 1) {
            throw new RegraDaCasaException("O ranking precisa de pelo menos 1 posição.");
        }
        Map<String, ProdutoNoRanking> porProduto = new HashMap<>();
        pedidosDo(filtro).stream()
                .filter(p -> p.getStatus() != StatusPedido.CANCELADO && p.getItens() != null)
                .flatMap(p -> p.getItens().stream())
                .forEach(item -> porProduto.merge(item.getProdutoId(),
                        new ProdutoNoRanking(item.getProdutoId(), item.getNomeProduto(), item.getQuantidade(),
                                Objects.requireNonNullElse(item.getSubtotal(), 0.0)),
                        ProdutoNoRanking::somar));

        return porProduto.values().stream()
                .sorted(Comparator.comparingLong(ProdutoNoRanking::unidades)
                        .thenComparingDouble(ProdutoNoRanking::receita).reversed())
                .limit(limite)
                .map(p -> new ProdutoNoRanking(p.produtoId(), p.nome(), p.unidades(), centavos(p.receita())))
                .toList();
    }

    public Map<DayOfWeek, long[]> movimentoPorHorario(FiltroPainel filtro) {
        Map<DayOfWeek, long[]> movimento = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek dia : DayOfWeek.values()) {
            movimento.put(dia, new long[24]);
        }
        pedidosDo(filtro).stream()
                .map(Pedido::getDataHoraEntrada)
                .filter(Objects::nonNull)
                .forEach(entrada -> movimento.get(entrada.getDayOfWeek())[entrada.getHour()]++);
        return movimento;
    }

    private List<Pedido> pedidosDo(FiltroPainel filtro) {
        List<Pedido> pedidos;
        if (filtro.temPeriodo()) {
            LocalDate inicio = Objects.requireNonNullElse(filtro.inicio(), LocalDate.EPOCH);
            LocalDate fim = Objects.requireNonNullElse(filtro.fim(), LocalDate.now());
            if (inicio.isAfter(fim)) {
                throw new RegraDaCasaException("O período informado termina antes de começar.");
            }
            pedidos = pedidoRepository.findByDataHoraEntradaBetween(
                    Range.rightOpen(inicio.atStartOfDay(), fim.plusDays(1).atStartOfDay()));
        } else {
            pedidos = pedidoRepository.findAll();
        }
        if (filtro.status() == null) {
            return pedidos;
        }
        return pedidos.stream().filter(p -> p.getStatus() == filtro.status()).toList();
    }

    private double centavos(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
