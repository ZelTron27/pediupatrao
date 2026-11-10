package com.umc.pediupatrao.dto;

import java.time.LocalDate;

public record VendaDoDia(LocalDate dia, long pedidos, double faturamento) {
}
