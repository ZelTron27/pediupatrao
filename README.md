# Pediu Patrão

Sistema de gestão de pedidos de uma pizzaria, evoluído a partir do projeto base da disciplina de Análise e Projeto de Software (Atividade Prática M1 - Parte 2).

O projeto original já cadastrava clientes, produtos e pedidos. Nesta versão foram adicionados três perfis de acesso com autorização no backend, o cálculo dos valores do pedido no servidor, o fluxo controlado de status com registro de entrada e saída, cancelamentos e descontos exclusivos do gerente, e uma trilha de auditoria persistente no MongoDB.

**Tecnologias:** Java 21+, Spring Boot 3.4, Spring Security, Spring Data MongoDB, Thymeleaf, Lombok, JUnit 5 e Mockito.

---

## Sumário

1. [Como executar](#como-executar)
2. [Usuários de acesso](#usuários-de-acesso)
3. [Perfis e permissões](#perfis-e-permissões)
4. [Requisitos implementados](#requisitos-implementados)
5. [Principais alterações no código](#principais-alterações-no-código)
6. [API](#api)
7. [Testes](#testes)
8. [Limitações conhecidas](#limitações-conhecidas)

---

## Como executar

### Pré-requisitos

- JDK 21 ou superior (o projeto foi desenvolvido e testado com o JDK 25)
- MongoDB rodando localmente na porta padrão `27017`

A conexão está em `src/main/resources/application.properties`:

```properties
spring.data.mongodb.uri=mongodb://127.0.0.1:27017/pizzaria
```

### Subindo a aplicação

No terminal, dentro da pasta do projeto:

```bash
./mvnw spring-boot:run
```

Depois é só acessar `http://localhost:8080`.

Na primeira execução, a `MongoInitConfig` cria as coleções que ainda não existirem e a classe `DataInitializer` cria os dados iniciais:

- os três usuários padrão (veja [Usuários de acesso](#usuários-de-acesso)), caso ainda não existam;
- os produtos usados nos pedidos, caso a coleção de produtos esteja vazia.

### Produtos iniciais

Como a tela de produtos não funciona (veja [Limitações conhecidas](#limitações-conhecidas)), os produtos já são cadastrados automaticamente ao subir a aplicação:

| Produto                        | Tipo   | Preço    |
|--------------------------------|--------|----------|
| Pizza Portuguesa (sem ervilha) | Pizza  | R$ 40,00 |
| Pizza 10 Queijos               | Pizza  | R$ 45,00 |
| Fanta Guaraná                  | Bebida | R$ 7,00  |

Clientes, pedidos e registros de auditoria devem ser criados pelo próprio sistema, pois é ele que calcula os valores e gera a auditoria de cada operação.

---

## Usuários de acesso

| Usuário     | Senha          | Perfil    |
|-------------|----------------|-----------|
| `admin`     | `adm!pizza67`  | ADMIN     |
| `gerente`   | `ger!pizza67`  | GERENTE   |
| `atendente` | `ate!pizza67`  | ATENDENTE |

As senhas são gravadas criptografadas com BCrypt. Os usuários só são criados se ainda não existirem no banco.

---

## Perfis e permissões

| Operação                                   | ADMIN | GERENTE | ATENDENTE |
|--------------------------------------------|:-----:|:-------:|:---------:|
| Administrar usuários e atribuir perfis     | X     |         |           |
| Consultar pedidos                          | X     | X       | X         |
| Registrar pedidos e avançar o status       |       | X       | X         |
| Cancelar pedidos                           |       | X       |           |
| Aplicar desconto (até 20%)                 |       | X       |           |
| Consultar clientes                         |       | X       | X         |
| Cadastrar e alterar clientes               |       |         | X         |
| Consultar a auditoria                      | X     | X       |           |

As regras são aplicadas no backend em duas camadas:

- **Rotas** (`SecurityConfig`): cada URL de tela e de API exige o perfil correspondente.
- **Serviços** (`@PreAuthorize`): os métodos de negócio verificam o perfil de novo, então a regra vale mesmo que a rota seja acessada de outra forma.

O menu lateral mostra apenas as telas que o perfil pode abrir, mas isso é só usabilidade. Quem tenta acessar uma tela sem permissão recebe a página de **acesso negado (403)**, e a API responde `403` com uma mensagem.

---

## Requisitos implementados

### RF01 - Controle de usuários e permissões

- Perfis `ADMIN`, `GERENTE` e `ATENDENTE` no enum `Perfil`, cada um com seu limite de desconto (somente o GERENTE tem 20%).
- `UsuarioDetailsService` carrega a role a partir do perfil gravado no banco. Usuário sem perfil não consegue entrar.
- Cadastro e alteração de usuários exclusivos do ADMIN.
- Proteção contra autoelevação e perda de acesso:
  - o usuário logado não pode alterar o próprio perfil nem excluir a própria conta;
  - o sistema não permite remover ou rebaixar o último ADMIN;
  - nomes de usuário repetidos são recusados.
- CSRF habilitado em todos os formulários e nas chamadas `fetch` (token enviado pelo cabeçalho).

### RF02 - Evolução do cadastro de pedidos

- O pedido passou a ter itens (`ItemPedido`) com produto, quantidade, preço unitário e subtotal, além de subtotal geral, percentual e valor de desconto, valor total, responsável e data/hora de entrada.
- **Todos os valores são calculados no servidor** a partir do preço cadastrado do produto. Valores de total, desconto, status ou preço enviados na requisição são ignorados.
- Validações: cliente existente, ao menos um item, quantidade maior que zero, produto existente, ativo e com preço válido.

### RF03 - Entrada, saída e transições

O fluxo permitido está definido no próprio enum `StatusPedido`:
1. `RECEBIDO` vai para `EM_PREPARACAO`
2. `EM_PREPARACAO` vai para `PRONTO`
3. `PRONTO` vai para `SAIU_PARA_ENTREGA` ou `RETIRADO`
4. `SAIU_PARA_ENTREGA` ou `RETIRADO` vai para `FINALIZADO`

O cancelamento (`CANCELADO`) só é permitido ao GERENTE e apenas nas etapas 1 a 3, antes da saída do pedido.
- Ao criar o pedido, são registrados a data/hora de entrada e o usuário responsável.
- Ao passar para `SAIU_PARA_ENTREGA` ou `RETIRADO`, são registrados obrigatoriamente a data/hora e o responsável pela saída.
- Qualquer transição fora do fluxo é recusada (ex: finalizar um pedido em preparação).
- Pedido cancelado ou finalizado não muda mais de status.

### RF04 - Cancelamentos e descontos

- **Cancelamento:** somente o GERENTE, com justificativa obrigatória e apenas antes da saída do pedido. Os dados do pedido são preservados e a justificativa fica gravada nele.
- **Desconto:** somente o GERENTE, percentual entre 0% e 20%, e apenas em pedidos `RECEBIDO`, `EM_PREPARACAO` ou `PRONTO`. O desconto é sempre calculado sobre o subtotal, então aplicar de novo substitui o anterior em vez de acumular.
- O ADMIN também é bloqueado nas duas operações.

### RF05 - Auditoria

Toda operação relevante grava um `EventoAuditoria` na coleção `auditoria`, por meio do `TrilhaAuditoriaService`:

| Evento              | Quando acontece                          |
|---------------------|------------------------------------------|
| `INCLUSAO_PEDIDO`   | Criação de pedido                        |
| `MUDANCA_STATUS`    | Avanço de status                         |
| `CANCELAMENTO`      | Cancelamento (com a justificativa)       |
| `DESCONTO`          | Aplicação de desconto                    |
| `INCLUSAO_CLIENTE`  | Cadastro de cliente                      |
| `ALTERACAO_CLIENTE` | Alteração de qualquer dado do cliente    |

Cada evento guarda o usuário e o perfil, a data/hora, o tipo de operação, a entidade e o seu identificador, a justificativa (quando houver) e a lista de **campos alterados com o valor anterior e o novo**. Na alteração de cliente, só entram os campos que realmente mudaram (endereço, telefone etc.).

Exemplo do que aparece na tela de auditoria:

| Quando           | Responsável | Evento            | O que mudou                            |
|------------------|-------------|-------------------|----------------------------------------|
| 23/09/2026 14:35 | atendente   | ALTERACAO_CLIENTE | logradouro: Rua das Flores -> Av. Brasil |

Integridade e proteção dos dados:

- A consulta (`/auditoria`) é exclusiva de ADMIN e GERENTE.
- O repositório da auditoria (`EventoAuditoriaRepository`) só tem os métodos de gravar e listar. Não existe rota nem método para editar ou excluir registros, e qualquer tentativa em `/auditoria/**` é bloqueada.
- Senhas não aparecem na auditoria nem nos logs. A senha também não é devolvida pela API de usuários.

---

## Principais alterações no código

| Arquivo                                            | O que mudou                                                         |
|----------------------------------------------------|---------------------------------------------------------------------|
| `entity/Perfil`, `entity/Usuario`                  | Três perfis com limite de desconto; usuário passa a ter um perfil  |
| `entity/Pedido`, `entity/ItemPedido`               | Itens, valores, responsáveis e datas de entrada/saída              |
| `entity/StatusPedido`                              | Fluxo de transições permitido                                      |
| `entity/EventoAuditoria`, `entity/TipoEvento`      | Modelo do registro de auditoria                                    |
| `config/SecurityConfig`                            | Regras de acesso por perfil e segurança por método                 |
| `service/PedidoService`                            | Cálculo de valores, transições, cancelamento e desconto            |
| `service/ClienteService`                           | Comparação dos campos alterados para a auditoria                   |
| `service/UsuarioService`                           | Regras contra autoelevação e remoção do último ADMIN               |
| `service/TrilhaAuditoriaService`                   | Gravação e consulta da auditoria                                   |
| `service/RegraDaCasaException`                     | Exceção das regras de negócio, mostrada como mensagem ao usuário   |
| `controller/TratamentoErrosApi`                    | Respostas da API com status (400, 403, 404) e mensagem em JSON     |
| `templates/pedidos.html`, `templates/auditoria.html` | Telas de pedidos (status, desconto, cancelamento) e de auditoria |
| `templates/fragments/sidebar.html`                 | Menu exibe só as telas permitidas ao perfil                         |

---

## API

Todas as rotas exigem login. Requisições que alteram dados precisam do token CSRF.

| Método | Rota                                                      | Perfis             |
|--------|-----------------------------------------------------------|--------------------|
| GET    | `/api/pedidos`                                            | todos              |
| POST   | `/api/pedidos`                                            | GERENTE, ATENDENTE |
| PUT    | `/api/pedidos/{id}/status?status=...&justificativa=...`   | GERENTE, ATENDENTE (cancelar: só GERENTE) |
| PATCH  | `/api/pedidos/{id}/desconto` - corpo `{"percentual": 10}` | GERENTE          |
| GET    | `/api/clientes`                                           | GERENTE, ATENDENTE |
| POST / PUT / DELETE | `/api/clientes`                                           | ATENDENTE          |
| GET / POST / PUT / DELETE | `/api/usuarios`                                           | ADMIN              |

Respostas de erro:

```json
{ "erro": "O desconto deve ficar entre 0% e 20%." }
```

- `400` - regra de negócio violada
- `403` - perfil sem permissão
- `404` - registro não encontrado

---

## Testes

```bash
./mvnw test
```

São 85 testes automatizados, todos passando:

- **Unitários (JUnit 5 + Mockito):** regras de `PedidoService`, `UsuarioService`, `ClienteService`, `ProdutoService`, `TrilhaAuditoriaService` e `UsuarioDetailsService`.
- **Integração (`AutorizacaoIntegrationTest`):** sobe o Spring com a segurança real e o MockMvc, com os repositórios simulados (não precisa do MongoDB rodando). Verifica, entre outros:
  - ADMIN e ATENDENTE bloqueados ao cancelar ou dar desconto, pela API e pelo formulário;
  - limite de 20% e justificativa obrigatória;
  - total do pedido recalculado no servidor;
  - acesso às APIs conforme o perfil e exigência de CSRF;
  - auditoria gravando usuário, perfil e valores anteriores e novos;
  - auditoria sem possibilidade de alteração ou exclusão;
  - menu exibido conforme o perfil.

---

## Limitações conhecidas

- **Tela de produtos com erro 500.** Ao abrir `/produtos` a aplicação mostra erro 500. O problema já existia no projeto base: o arquivo `produtos/lista.html` é uma página completa e não tem o fragmento `content` que o `layout.html` espera, diferente das demais telas. Além disso, o formulário dessa tela envia os dados para a API, que espera JSON, e os botões de editar e excluir apontam para rotas que não existem. Como o cadastro de produtos não faz parte dos requisitos da atividade, a tela foi mantida como estava, e os produtos são criados automaticamente pelo `DataInitializer` (veja [Produtos iniciais](#produtos-iniciais)).
- **Produtos sem restrição de perfil.** A API de produtos (`/api/produtos`) aceita qualquer usuário logado, porque o enunciado não define qual perfil é responsável por eles.
- **Itens herdados do projeto base:** as páginas `/admin` e `/teste` e os itens de menu Funcionários, Entregadores e Horário de Funcionamento não fazem parte desta atividade e não estão implementados.
