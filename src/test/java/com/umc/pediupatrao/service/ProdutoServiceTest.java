package com.umc.pediupatrao.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.repository.ProdutoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {

    @Mock
    private ProdutoRepository produtoRepository;

    @InjectMocks
    private ProdutoService produtoService;

    @Test
    void salvarProdutoSemIdCriaNovo() {
        Produto produto = new Produto();
        when(produtoRepository.save(produto)).thenReturn(produto);

        assertSame(produto, produtoService.salvar(produto));
        verify(produtoRepository, never()).existsById(any());
    }

    @Test
    void salvarProdutoComIdExistenteAtualiza() {
        Produto produto = new Produto();
        produto.setId("pr1");
        when(produtoRepository.existsById("pr1")).thenReturn(true);
        when(produtoRepository.save(produto)).thenReturn(produto);

        assertSame(produto, produtoService.salvar(produto));
    }

    @Test
    void salvarProdutoComIdInexistenteLancaErro() {
        Produto produto = new Produto();
        produto.setId("pr1");
        when(produtoRepository.existsById("pr1")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> produtoService.salvar(produto));
        verify(produtoRepository, never()).save(produto);
    }
}
