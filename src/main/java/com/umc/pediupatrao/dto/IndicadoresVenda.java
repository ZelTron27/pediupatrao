package com.umc.pediupatrao.dto;

public record IndicadoresVenda(double faturamento, long pedidosFaturados, double ticketMedio,
        long cancelados, double somaDescontos) {
}
