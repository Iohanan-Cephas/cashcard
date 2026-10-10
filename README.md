# Cash Card API

API REST construída com Spring Boot para gerenciar "cash cards" (cartões de saldo). Cada cartão guarda um valor monetário e pertence a um usuário autenticado, que só pode ler, alterar e excluir os seus próprios cartões.

## Funcionalidades

- **Cadastro de cartões** — cria um novo cartão com um saldo, atribuindo automaticamente como dono o usuário autenticado na requisição.
- **Consulta individual e em lista** — recupera um cartão pelo id ou lista todos os cartões do usuário, com suporte a paginação e ordenação.
- **Atualização** — altera o saldo de um cartão existente.
- **Exclusão** — remove um cartão.
- **Controle de acesso** — autenticação HTTP Basic com perfis em memória; usuários sem o role `CARD-OWNER` são bloqueados, e cada usuário só enxerga os cartões que possui (tentativas de acesso a cartões de terceiros retornam `404`, sem revelar a existência do recurso).

## Stack tecnológica

| Componente | Tecnologia |
| --- | --- |
| Linguagem | Java 21 |
| Framework | Spring Boot 4.1.1 |
| Segurança | Spring Security (HTTP Basic + BCrypt) |
| Persistência | Spring Data JDBC + H2 (em memória) |
| Build | Gradle |

## Endpoints

Base path: `/cashcards` — todos exigem autenticação e o role `CARD-OWNER`.

| Método | Rota | Descrição | Sucesso |
| --- | --- | --- | --- |
| `GET` | `/cashcards` | Lista os cartões do usuário autenticado | `200` |
| `GET` | `/cashcards/{id}` | Busca um cartão pelo id | `200` |
| `POST` | `/cashcards` | Cria um cartão (o dono vem do token autenticado) | `201` + `Location` |
| `PUT` | `/cashcards/{id}` | Atualiza o saldo de um cartão | `204` |
| `DELETE` | `/cashcards/{id}` | Exclui um cartão | `204` |

Respostas de erro: `401` (credenciais inválidas), `403` (usuário sem o role adequado), `404` (cartão inexistente ou pertencente a outro usuário).

### Parâmetros de listação

`GET /cashcards` aceita os parâmetros de paginação e ordenação do Spring Data:

- `page` — índice da página (inicia em 0)
- `size` — tamanho da página
- `sort=campo,direcao` — ex.: `sort=amount,desc`

Sem parâmetros, a listagem usa ordenação padrão por `amount` ascendente.

### Exemplos

```bash
# Listar cartões (ordenados por saldo crescente)
curl -u sarah1:abc123 http://localhost:8080/cashcards

# Paginar e ordenar
curl -u sarah1:abc123 "http://localhost:8080/cashcards?page=0&size=1&sort=amount,desc"

# Buscar um cartão
curl -u sarah1:abc123 http://localhost:8080/cashcards/99

# Criar um cartão
curl -u sarah1:abc123 -X POST http://localhost:8080/cashcards \
  -H "Content-Type: application/json" \
  -d '{"amount": 250.00}'

# Atualizar o saldo
curl -u sarah1:abc123 -X PUT http://localhost:8080/cashcards/99 \
  -H "Content-Type: application/json" \
  -d '{"amount": 19.99}'

# Excluir
curl -u sarah1:abc123 -X DELETE http://localhost:8080/cashcards/99
```

## Modelo de dados

A entidade é um record imutável (`example.cashcard.models.CashCard`) mapeado para a tabela `cash_card`:

| Coluna | Tipo | Restrição |
| --- | --- | --- |
| `ID` | BIGINT | PK, gerada automaticamente |
| `AMOUNT` | NUMBER | obrigatório, padrão 0 |
| `OWNER` | VARCHAR(256) | obrigatório |

O schema é criado por `src/main/resources/schema.sql` ao subir a aplicação.

## Segurança

Definida em `SecurityConfig`:

- Todas as rotas `/cashcards/**` exigem o role `CARD-OWNER`.
- Autenticação HTTP Basic com senhas codificadas via BCrypt.
- CSRF desabilitado (API sem sessão/cookies).
- Usuários em memória (apenas para testes):

| Usuário | Senha | Role |
| --- | --- | --- |
| `sarah1` | `abc123` | `CARD-OWNER` |
| `kumar2` | `ghi789` | `CARD-OWNER` |
| `hank-owns-no-cards` | `def456` | `NON-OWNER` |

## Estrutura do projeto

```
src/main/java/example/cashcard/
├── CashCardApplication.java          # classe principal
├── config/SecurityConfig.java        # segurança e usuários em memória
├── controllers/CashCardController.java  # endpoints REST
├── models/CashCard.java              # entidade (record)
└── repositories/CashCardRepository.java # acesso a dados (Spring Data JDBC)
```

O repositório (`CashCardRepository`) expõe as consultas por dono que garantem o isolamento entre usuários: `findByIdAndOwner`, `findByOwner` e `existsByIdAndOwner`.

## Rodando o projeto

```bash
# Iniciar a aplicação (porta 8080)
./gradlew bootRun

# Executar os testes
./gradlew test
```

## Testes

O projeto possui 20 testes, todos verdes:

- **`CashCardApplicationTests`** (16 testes de integração) — cobrem a API de ponta a ponta via `TestRestTemplate`: busca, criação, listagem paginada/ordenada, atualização e exclusão, além dos cenários de segurança (credenciais inválidas → `401`, role errado → `403`, acesso a cartão de outro usuário → `404`).
- **`CashCardJsonTest`** (4 testes de serialização) — validam a conversão JSON de um cartão e de uma lista de cartões, comparando com os arquivos em `src/test/resources/example/cashcard/`.

Os dados de teste vêm de `src/test/resources/data.sql`:

| ID | Amount | Owner |
| --- | --- | --- |
| 99 | 123.45 | sarah1 |
| 100 | 1.00 | sarah1 |
| 101 | 150.00 | sarah1 |
| 102 | 200.00 | kumar2 |
