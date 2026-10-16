package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.ProdutoRepository;
import com.umc.pediupatrao.repository.UsuarioRepository;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(UsuarioRepository repo, ProdutoRepository produtoRepo) {
        return args -> {

            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

            if (repo.findByUsername("admin").isEmpty()) {

                Usuario user = new Usuario();
                user.setUsername("admin");
                user.setPassword(encoder.encode("adm!pizza67"));
                user.setPerfil(Perfil.ADMIN);

                repo.save(user);

                System.out.println("Usuário admin criado!");
            }

            repo.findByUsername("admin")
                    .filter(admin -> admin.getPerfil() == null)
                    .ifPresent(admin -> {
                        admin.setPerfil(Perfil.ADMIN);
                        repo.save(admin);
                    });

            if (repo.findByUsername("gerente").isEmpty()) {

                Usuario user = new Usuario();
                user.setUsername("gerente");
                user.setPassword(encoder.encode("ger!pizza67"));
                user.setPerfil(Perfil.GERENTE);

                repo.save(user);

                System.out.println("Usuário gerente criado!");
            }

            if (repo.findByUsername("atendente").isEmpty()) {

                Usuario user = new Usuario();
                user.setUsername("atendente");
                user.setPassword(encoder.encode("ate!pizza67"));
                user.setPerfil(Perfil.ATENDENTE);

                repo.save(user);

                System.out.println("Usuário atendente criado!");
            }

            if (produtoRepo.count() == 0) {

                produtoRepo.saveAll(List.of(
                        produto("Pizza Portuguesa (sem ervilha)", "Pizza", 40.0),
                        produto("Pizza 10 Queijos", "Pizza", 45.0),
                        produto("Fanta Guaraná", "Bebida", 7.0)));

                System.out.println("Produtos iniciais criados!");
            }
        };
    }

    private Produto produto(String nome, String tipo, Double preco) {
        Produto produto = new Produto();
        produto.setNome(nome);
        produto.setTipo(tipo);
        produto.setPreco(preco);
        produto.setAtivo(true);
        return produto;
    }
}
