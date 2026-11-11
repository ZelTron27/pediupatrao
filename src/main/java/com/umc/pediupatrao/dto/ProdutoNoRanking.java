package com.umc.pediupatrao.dto;

public record ProdutoNoRanking(String produtoId, String nome, long unidades, double receita) {

    public ProdutoNoRanking somar(ProdutoNoRanking outro) {
        return new ProdutoNoRanking(produtoId, nome, unidades + outro.unidades, receita + outro.receita);
    }
}
