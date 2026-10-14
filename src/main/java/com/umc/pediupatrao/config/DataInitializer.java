package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(UsuarioRepository repo) {
        return args -> {

            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

            if (repo.findByUsername("admin").isEmpty()) {

                Usuario user = new Usuario();
                user.setUsername("admin");
                user.setPassword(encoder.encode("teste123"));
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
                user.setPassword(encoder.encode("pizza@gerente"));
                user.setPerfil(Perfil.GERENTE);

                repo.save(user);

                System.out.println("Usuário gerente criado!");
            }

            if (repo.findByUsername("atendente").isEmpty()) {

                Usuario user = new Usuario();
                user.setUsername("atendente");
                user.setPassword(encoder.encode("pizza@atendente"));
                user.setPerfil(Perfil.ATENDENTE);

                repo.save(user);

                System.out.println("Usuário atendente criado!");
            }
        };
    }
}
