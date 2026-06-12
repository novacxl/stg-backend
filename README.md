# 🛍️ STGIAN — E-commerce Streetwear

> E-commerce completo para startup de streetwear nordestina.
> Backend Spring Boot com autenticação JWT, pagamentos via Mercado Pago,
> rastreio automático via API dos Correios e painel administrativo completo.

🔗 **Site em produção:** https://stgian.com.br

## 🛠️ Stack

**Backend:** Java 17 · Spring Boot 3.2 · Spring Security · JWT · JPA/Hibernate · MySQL
**Integrações:** Mercado Pago API · API Correios · Railway (deploy)
**Frontend:** HTML5 · CSS3 · JavaScript (Vanilla) · Netlify (deploy)

## ✨ Funcionalidades

- Autenticação com 3 níveis de acesso (CLIENT / ADMIN / OWNER)
- Catálogo com filtro por categoria e sistema de wishlist
- Checkout completo com endereço, CPF e integração Mercado Pago
- Pagamento via PIX, crédito (12x) e débito
- Webhook para atualização automática de status de pedidos
- Rastreio integrado com API dos Correios
- Painel ADM: produtos, estoque, pedidos, equipe e drops
- Sistema de drops com countdown regressivo

## 🔒 Segurança

- JWT stateless com secret 256 bits
- BCrypt força 12
- Rate limiting por email + IP
- Validação matemática de CPF

## ⚙️ Como rodar localmente

```bash
git clone https://github.com/SEU-USUARIO/stgian-backend
# Configure application.properties com base no application.properties.example
mvn spring-boot:run
# Abra stgian_integrado.html com Live Server
```
