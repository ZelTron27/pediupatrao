package com.umc.pediupatrao.entity;

import jakarta.persistence.Entity;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "pedidos")
@Entity
public class Pedido {
    @Id
    private String id;
    private String clienteId;
    private String tipoPizza;
    private String status;
    private int quantidade;
    private String sabor;
    private List<ItemPedido> itens = new ArrayList<>();
    private Double subtotal;
    private Double percentualDesconto;
    private Double valorDesconto;
    private Double valorTotal;
    private String responsavel;
    private LocalDateTime dataHoraEntrada;
}
