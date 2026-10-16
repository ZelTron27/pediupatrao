package com.umc.pediupatrao.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemPedido {

    private String produtoId;
    private String nomeProduto;
    private int quantidade;
    private Double precoUnitario;
    private Double subtotal;
}
