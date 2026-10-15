package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
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

@Service
public class PedidoService {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

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
        return pedidoRepository.save(pedido);
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

    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
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
        if (novoStatus == StatusPedido.CANCELADO) {
            boolean gerente = usuario.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_GERENTE"));
            if (!gerente) {
                throw new RegraDaCasaException("Somente o GERENTE pode cancelar pedidos.");
            }
            if (justificativa == null || justificativa.isBlank()) {
                throw new RegraDaCasaException("Informe a justificativa do cancelamento.");
            }
            pedido.setJustificativaCancelamento(justificativa.trim());
        }
        if (novoStatus == StatusPedido.SAIU_PARA_ENTREGA || novoStatus == StatusPedido.RETIRADO) {
            pedido.setResponsavelSaida(usuario.getName());
            pedido.setDataHoraSaida(LocalDateTime.now());
        }

        pedido.setStatus(novoStatus);
        return pedidoRepository.save(pedido);
    }

    private Authentication usuarioLogado() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

}
