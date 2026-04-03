package com.stgian.config;

import com.stgian.model.Drop;
import com.stgian.model.Product;
import com.stgian.model.User;
import com.stgian.repository.DropRepository;
import com.stgian.repository.ProductRepository;
import com.stgian.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = Logger.getLogger(DataInitializer.class.getName());

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final DropRepository dropRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           ProductRepository productRepository,
                           DropRepository dropRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository    = userRepository;
        this.productRepository = productRepository;
        this.dropRepository    = dropRepository;
        this.passwordEncoder   = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        // FIX-CRITICO-4: Cria o usuário OWNER se não existir nenhum
        // Sem um OWNER, ninguém consegue acessar o painel de gestão
        if (userRepository.findByRole(User.Role.OWNER).isEmpty()) {
            User owner = User.builder()
                .name("Lindomar Silva")
                .email("lindomarggdsilva@gmail.com")
                .password(passwordEncoder.encode("91715852gG@"))
                .role(User.Role.OWNER)
                .build();
            userRepository.save(owner);
            log.info("OWNER criado: lindomarggdsilva@gmail.com");
        }

        // ── Produtos padrão ──
        if (productRepository.count() == 0) {
            List<Product> products = List.of(
                Product.builder().name("Camiseta STGIAN Origin")
                    .description("100% algodao pesado - Oversized")
                    .price(89).category(Product.Category.camisetas)
                    .badge(Product.Badge.new_).stock(15).icon("S").build(),

                Product.builder().name("Camiseta Broken City")
                    .description("Estampa serigrafada - Preta")
                    .price(99).category(Product.Category.camisetas)
                    .stock(8).icon("B").build(),

                Product.builder().name("Camiseta No Signal")
                    .description("Grafico exclusivo - Drop 001")
                    .price(94).category(Product.Category.camisetas)
                    .badge(Product.Badge.hot).stock(3).icon("N").build(),

                Product.builder().name("Moletom STGIAN Classic")
                    .description("Felpado pesado - Logo bordado")
                    .price(189).category(Product.Category.moletons)
                    .badge(Product.Badge.hot).stock(2).icon("M").build(),

                Product.builder().name("Moletom Periferia")
                    .description("Oversized - Capuz duplo")
                    .price(199).category(Product.Category.moletons)
                    .badge(Product.Badge.new_).stock(10).icon("P").build(),

                Product.builder().name("Bone STGIAN Snapback")
                    .description("Ajuste universal - Bordado")
                    .price(89).category(Product.Category.bones)
                    .stock(20).icon("B").build()
            );
            productRepository.saveAll(products);
            log.info("6 produtos criados.");
        }

        // ── Drop padrão ──
        if (dropRepository.count() == 0) {
            Drop drop = new Drop();
            drop.setActive(true);
            drop.setTag("Em breve · Drop 002");
            drop.setTitle1("COLEÇÃO");
            drop.setTitle2("ASFALTO");
            drop.setDescription("A segunda coleção da STGIAN. Paleta escura, gráficos pesados, cortes oversized.");
            drop.setBgNum("002");
            drop.setLaunchDate(LocalDateTime.now().plusDays(30));
            drop.setMini1Name("Moletom STGIAN Classic");
            drop.setMini1Price(189);
            drop.setMini1Desc("Últimas unidades do Drop 001");
            drop.setMini1Tag("Esgotando");
            drop.setMini2Name("Boné STGIAN Snapback");
            drop.setMini2Price(89);
            drop.setMini2Desc("Ajuste universal · Logo bordado");
            drop.setMini2Tag("Disponível");
            dropRepository.save(drop);
            log.info("Drop inicial criado.");
        }
    }
}
