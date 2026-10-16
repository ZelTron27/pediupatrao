package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.DescontoPedido;
import com.umc.pediupatrao.entity.EventoAuditoria.CampoAlterado;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoEvento;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PedidoService {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private TrilhaAuditoria trilhaAuditoria;

    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    public Pedido criarPedido(Pedido pedido) {
        if (pedido.getClienteId() == null || !clienteRepository.existsById(pedido.getClienteId())) {
            throw new RegraDaCasaException("Cliente não encontrado.");
        }
        if (pedido.getItens() == null || pedido.getItens().isEmpty()) {
            throw new RegraDaCasaException("O pedido precisa ter ao menos um item.");
        }

        List<ItemPedido> itens = new ArrayList<>();
        double subtotal = 0;
        for (ItemPedido enviado : pedido.getItens()) {
            ItemPedido item = montarItem(enviado);
            itens.add(item);
            subtotal += item.getSubtotal();
        }

        pedido.setId(null);
        pedido.setItens(itens);
        pedido.setSubtotal(arredondar(subtotal));
        pedido.setPercentualDesconto(0.0);
        pedido.setValorDesconto(0.0);
        pedido.setValorTotal(arredondar(pedido.getSubtotal() - pedido.getValorDesconto()));
        pedido.setResponsavel(usuarioLogado().getName());
        pedido.setDataHoraEntrada(LocalDateTime.now());
        pedido.setStatus(StatusPedido.RECEBIDO);
        pedido.setResponsavelSaida(null);
        pedido.setDataHoraSaida(null);
        pedido.setJustificativaCancelamento(null);
        Pedido salvo = pedidoRepository.save(pedido);

        String resumoItens = salvo.getItens().stream()
                .map(i -> i.getQuantidade() + "x " + i.getNomeProduto())
                .collect(Collectors.joining(", "));
        trilhaAuditoria.registrar(TipoEvento.INCLUSAO_PEDIDO, "Pedido", salvo.getId(), null, List.of(
                CampoAlterado.de("clienteId", null, salvo.getClienteId()),
                CampoAlterado.de("itens", null, resumoItens),
                CampoAlterado.de("valorTotal", null, salvo.getValorTotal()),
                CampoAlterado.de("status", null, salvo.getStatus())));
        return salvo;
    }

    private ItemPedido montarItem(ItemPedido enviado) {
        if (enviado.getQuantidade() <= 0) {
            throw new RegraDaCasaException("A quantidade de cada item deve ser maior que zero.");
        }
        if (enviado.getProdutoId() == null) {
            throw new RegraDaCasaException("Produto não encontrado.");
        }
        Produto produto = produtoRepository.findById(enviado.getProdutoId())
                .orElseThrow(() -> new RegraDaCasaException("Produto não encontrado."));
        if (Boolean.FALSE.equals(produto.getAtivo())) {
            throw new RegraDaCasaException("O produto " + produto.getNome() + " está inativo.");
        }
        if (produto.getPreco() == null || produto.getPreco() <= 0) {
            throw new RegraDaCasaException("O produto " + produto.getNome() + " está sem preço válido.");
        }

        ItemPedido item = new ItemPedido();
        item.setProdutoId(produto.getId());
        item.setNomeProduto(produto.getNome());
        item.setQuantidade(enviado.getQuantidade());
        item.setPrecoUnitario(produto.getPreco());
        item.setSubtotal(arredondar(produto.getPreco() * enviado.getQuantidade()));
        return item;
    }

    private double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll();
    }

    @PreAuthorize("hasRole('GERENTE') or (hasRole('ATENDENTE') and #novoStatus?.name() != 'CANCELADO')")
    public Pedido atualizarStatus(String id, StatusPedido novoStatus, String justificativa) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RegraDaCasaException("Pedido não encontrado."));
        if (novoStatus == null) {
            throw new RegraDaCasaException("Informe o novo status do pedido.");
        }
        if (pedido.getStatus() == StatusPedido.CANCELADO) {
            throw new RegraDaCasaException("Pedido cancelado não pode mudar de status.");
        }
        if (pedido.getStatus() == null || !pedido.getStatus().proximos().contains(novoStatus)) {
            throw new RegraDaCasaException("Não é permitido passar o pedido de "
                    + pedido.getStatus() + " para " + novoStatus + ".");
        }

        Authentication usuario = usuarioLogado();
        StatusPedido statusAnterior = pedido.getStatus();
        if (novoStatus == StatusPedido.CANCELADO) {
            if (justificativa == null || justificativa.isBlank()) {
                throw new RegraDaCasaException("Informe a justificativa do cancelamento.");
            }
            pedido.setJustificativaCancelamento(justificativa.trim());
        } else if (justificativa != null && !justificativa.isBlank()) {
            throw new RegraDaCasaException("Para cancelar, escolha CANCELADO no status. A justificativa só é usada no cancelamento.");
        }
        List<CampoAlterado> campos = new ArrayList<>();
        campos.add(CampoAlterado.de("status", statusAnterior, novoStatus));
        if (novoStatus == StatusPedido.SAIU_PARA_ENTREGA || novoStatus == StatusPedido.RETIRADO) {
            pedido.setResponsavelSaida(usuario.getName());
            pedido.setDataHoraSaida(LocalDateTime.now());
            campos.add(CampoAlterado.de("responsavelSaida", null, pedido.getResponsavelSaida()));
            campos.add(CampoAlterado.de("dataHoraSaida", null, pedido.getDataHoraSaida()));
        }

        pedido.setStatus(novoStatus);
        Pedido salvo = pedidoRepository.save(pedido);
        trilhaAuditoria.registrar(
                novoStatus == StatusPedido.CANCELADO ? TipoEvento.CANCELAMENTO : TipoEvento.MUDANCA_STATUS,
                "Pedido", salvo.getId(), salvo.getJustificativaCancelamento(), campos);
        return salvo;
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Pedido aplicarDesconto(String id, DescontoPedido desconto) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RegraDaCasaException("Pedido não encontrado."));
        int limite = limiteDesconto();
        if (desconto == null || desconto.percentual() == null
                || desconto.percentual() < 0 || desconto.percentual() > limite) {
            throw new RegraDaCasaException("O desconto deve ficar entre 0% e " + limite + "%.");
        }
        if (pedido.getStatus() == null || !pedido.getStatus().proximos().contains(StatusPedido.CANCELADO)) {
            throw new RegraDaCasaException("Desconto só pode ser dado em pedido RECEBIDO, EM_PREPARACAO ou PRONTO.");
        }
        if (pedido.getSubtotal() == null) {
            throw new RegraDaCasaException("Pedido sem valores calculados.");
        }

        Double percentualAnterior = pedido.getPercentualDesconto();
        Double descontoAnterior = pedido.getValorDesconto();
        Double totalAnterior = pedido.getValorTotal();

        pedido.setPercentualDesconto(desconto.percentual());
        pedido.setValorDesconto(arredondar(pedido.getSubtotal() * desconto.percentual() / 100));
        pedido.setValorTotal(arredondar(pedido.getSubtotal() - pedido.getValorDesconto()));
        Pedido salvo = pedidoRepository.save(pedido);

        trilhaAuditoria.registrar(TipoEvento.DESCONTO, "Pedido", salvo.getId(), null, List.of(
                CampoAlterado.de("percentualDesconto", percentualAnterior, salvo.getPercentualDesconto()),
                CampoAlterado.de("valorDesconto", descontoAnterior, salvo.getValorDesconto()),
                CampoAlterado.de("valorTotal", totalAnterior, salvo.getValorTotal())));
        return salvo;
    }

    public int limiteDesconto() {
        return usuarioLogado().getAuthorities().stream()
                .map(a -> Perfil.valueOf(a.getAuthority().replace("ROLE_", "")).getLimiteDesconto())
                .findFirst()
                .orElse(0);
    }

    private Authentication usuarioLogado() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

}
