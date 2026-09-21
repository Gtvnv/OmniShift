# OmniShift (motor de Orquestração de dados) 🔄

> **Universal Data Transformation Engine**
> Uma iniciativa [ZenithCode](https://github.com/gtvnv) mantida pela divisão P.O.N.T.E.

O OmniShift é um middleware de alta performance desenhado para orquestrar e transformar dados entre diferentes sistemas e formatos de forma agnóstica. Construído sob os princípios da **Arquitetura Hexagonal (Ports and Adapters)**, ele garante isolamento absoluto das regras de negócio, permitindo plugar novas tecnologias de entrada e saída sem atrito.

---

## 🏗️ Arquitetura

O coração do OmniShift é o seu **Modelo Canônico (`OmniNode`)**. Ao invés de converter um formato diretamente para outro (ex: JSON para XML), o sistema traduz qualquer formato de entrada para uma árvore de objetos em memória, e depois serializa essa árvore para o formato de saída desejado.

* **Inbound Adapters (Entrada):** REST (Spring Web), gRPC.
* **Outbound Adapters (Saída):** Jackson JSON, Jackson XML, Jackson YAML, Apache Commons CSV.
* **Core (Domínio):** Regras de conversão e orquestração totalmente independentes de frameworks externos.

### Módulos Maven

O projeto é organizado como um multi-módulo Maven, para que o núcleo de domínio não dependa de nenhum framework e novos formatos possam ser adicionados como plugins isolados:

| Módulo | Responsabilidade | Depende de |
|---|---|---|
| `omnishift-core` | Modelo canônico (`OmniNode`), portas (`DataParser`/`DataSerializer`), orquestração (`ShiftDataUseCase`). Zero dependência de Spring/Jackson. | — |
| `omnishift-grpc-api` | Contrato gRPC (`.proto`) e stubs gerados — reutilizável por clientes em qualquer linguagem que fale Protobuf. | — |
| `omnishift-adapter-jackson-common` | Conversão `OmniNode` ↔ `JsonNode`/`Object` compartilhada pelos três adapters abaixo (todos usam Jackson por baixo). Não implementa `DataParser`/`DataSerializer` nem se registra via SPI — existe só para eliminar duplicação entre os adapters. | `omnishift-core` |
| `omnishift-adapter-json` | `DataParser`/`DataSerializer` de JSON via Jackson. | `omnishift-core`, `omnishift-adapter-jackson-common` |
| `omnishift-adapter-xml` | `DataParser`/`DataSerializer` de XML via Jackson. | `omnishift-core`, `omnishift-adapter-jackson-common` |
| `omnishift-adapter-yaml` | `DataParser`/`DataSerializer` de YAML via Jackson. | `omnishift-core`, `omnishift-adapter-jackson-common` |
| `omnishift-adapter-csv` | `DataParser`/`DataSerializer` de CSV via Apache Commons CSV. Não usa Jackson (CSV é tabular, não passa por `JsonNode`). | `omnishift-core` |
| `omnishift-adapter-sql` | Só `DataSerializer` (gera SQL, não interpreta) — um por dialeto (`SQL_MYSQL`/`SQL_POSTGRESQL`/`SQL_ORACLE`/`SQL_SQLSERVER`), gerando `INSERT`. Sem dependência externa. | `omnishift-core` |
| `omnishift-runtime-spring` | Runtime executável: expõe o core via REST e gRPC (Spring Boot), montando os adapters descobertos automaticamente. | todos acima |

Os adapters de formato **não são referenciados por nome** em nenhum lugar do código central: eles se registram via [Java SPI](https://docs.oracle.com/javase/tutorial/ext/basics/spi.html) (`META-INF/services`) e são descobertos em tempo de execução por `ParserFactory.discover()`/`SerializerFactory.discover()`.

#### Como adicionar um novo formato

1. Crie um módulo Maven novo (ex: `omnishift-adapter-sql`) dependendo apenas de `omnishift-core`.
2. Implemente `DataParser` e/ou `DataSerializer` para o novo formato.
3. Declare a implementação em `src/main/resources/META-INF/services/br.com.github.gtvnv.omnishift.domain.ports.DataParser` (e/ou `DataSerializer`).
4. Adicione o novo módulo como dependência de `omnishift-runtime-spring`.

Nenhuma classe central precisa ser editada para o novo formato passar a ser reconhecido pelos headers `X-Source-Format`/`X-Target-Format`.

---

## 🚀 Tecnologias Utilizadas

* **Java 21** (Utilizando *Pattern Matching* e *Records*)
* **Spring Boot 3.3**
* **gRPC & Protobuf** (Comunicação de alta performance)
* **Jackson** (Serialização de dados)
* **JUnit 5** (Testes do `omnishift-core`)
* **Maven**

---

## ⚙️ Como Executar (Setup Local)

### Pré-requisitos
* Java Development Kit (JDK) 21
* Maven 3.8+ instalado (ou utilize o Maven Wrapper `mvnw` incluso no projeto)

---

### Compilação
O projeto é um multi-módulo Maven (reactor). O primeiro build precisa instalar os módulos internos (`omnishift-core`, `omnishift-grpc-api`, etc.) no repositório local antes que `omnishift-runtime-spring` consiga resolvê-los, e também é quando as classes do Protobuf são geradas:
```bash
mvn clean install
```

---

### Subindo a Aplicação
Execute o módulo de runtime via Maven, a partir da raiz do projeto:

```bash
mvn -pl omnishift-runtime-spring -am spring-boot:run
```

O servidor será iniciado na porta padrão 8080 para REST e 9090 para chamadas gRPC.

---

### Rodando via Docker

O `Dockerfile` é multi-stage: compila o reactor completo dentro do próprio container (não depende de um `target/` pré-existente no host) e a imagem final só carrega o JRE, rodando como usuário não-root.

```bash
docker build -t omnishift .
docker run -p 8080:8080 -p 9090:9090 omnishift
```

---

### Rodando os Testes
```bash
mvn -pl omnishift-core -am test
```

---

## 🧪 Testando a Aplicação (REST)
O OmniShift utiliza cabeçalhos HTTP (Headers) para identificar dinamicamente os formatos de origem e destino, mantendo o payload (corpo da requisição) limpo.

### Exemplo 1: Conversão JSON ➡️ XML

* **Requisição**:
```http
POST /api/v1/shift HTTP/1.1
Host: localhost:8080
X-Source-Format: JSON
X-Target-Format: XML
Content-Type: text/plain

{
  "usuario": {
    "nome": "Gustavo Tavera",
    "idade": 20,
    "divisao": "P.O.N.T.E",
    "habilidades": ["Java 21", "Arquitetura Hexagonal"]
  }
}
```

### Resposta:
```xml
<OmniShiftDocument>
  <usuario>
    <nome>Gustavo Tavera</nome>
    <idade>20</idade>
    <divisao>P.O.N.T.E</divisao>
    <habilidades>
      <habilidades>Java 21</habilidades>
      <habilidades>Arquitetura Hexagonal</habilidades>
    </habilidades>
  </usuario>
</OmniShiftDocument>
```

---

### Exemplo 2: Conversão XML ➡️ JSON
* **Requisição**:

```http
POST /api/v1/shift HTTP/1.1
Host: localhost:8080
X-Source-Format: XML
X-Target-Format: JSON
Content-Type: text/plain

<cliente>
    <id>991</id>
    <status>ativo</status>
</cliente>
```

### Resposta
```json
{
  "cliente": {
    "id": 991,
    "status": "ativo"
  }
}
```

---

### Exemplo 3: Conversão JSON ➡️ YAML

* **Requisição**:
```http
POST /api/v1/shift HTTP/1.1
Host: localhost:8080
X-Source-Format: JSON
X-Target-Format: YAML
Content-Type: text/plain

{
  "usuario": {
    "nome": "Gustavo Tavera",
    "idade": 20,
    "divisao": "P.O.N.T.E"
  }
}
```

### Resposta
```yaml
usuario:
  nome: "Gustavo Tavera"
  idade: 20
  divisao: "P.O.N.T.E"
```

---

### Exemplo 4: Conversão JSON ➡️ CSV

CSV é tabular, não uma árvore — a convenção do OmniShift é: o nível raiz deve ser um **array de objetos "flat"** (sem valores aninhados), cada objeto vira uma linha, e o cabeçalho é a união das chaves de todos os objetos. Nenhuma inferência de tipo acontece na leitura (todo valor CSV é lido como texto puro).

* **Requisição**:
```http
POST /api/v1/shift HTTP/1.1
Host: localhost:8080
X-Source-Format: JSON
X-Target-Format: CSV
Content-Type: text/plain

[
  { "nome": "Gustavo Tavera", "idade": 20 },
  { "nome": "Zenith Code", "cidade": "Recife" }
]
```

### Resposta
```csv
nome,idade,cidade
Gustavo Tavera,20,
Zenith Code,,Recife
```

---

### Exemplo 5: Conversão JSON ➡️ SQL (MySQL)

SQL não é uma árvore nem uma tabela — é geração de texto, e um `INSERT` não existe sem tabela alvo. Por isso o formato SQL exige uma forma própria na raiz: `{ "table": "...", "rows": [...] }` (cada elemento de `rows` é uma linha, mesma regra do CSV: sem valor aninhado numa célula). Dialetos suportados como formatos de destino separados: `SQL_MYSQL`, `SQL_POSTGRESQL`, `SQL_ORACLE`, `SQL_SQLSERVER` — só geração (`X-Source-Format: SQL_*` não existe, não há parser de volta).

* **Requisição**:
```http
POST /api/v1/shift HTTP/1.1
Host: localhost:8080
X-Source-Format: JSON
X-Target-Format: SQL_MYSQL
Content-Type: text/plain

{
  "table": "usuarios",
  "rows": [
    { "nome": "Gustavo Tavera", "idade": 20, "ativo": true },
    { "nome": "Zenith Code", "idade": 5, "ativo": false }
  ]
}
```

### Resposta
```sql
INSERT INTO `usuarios` (`nome`, `idade`, `ativo`) VALUES ('Gustavo Tavera', 20, TRUE);
INSERT INTO `usuarios` (`nome`, `idade`, `ativo`) VALUES ('Zenith Code', 5, FALSE);
```

Trocando `X-Target-Format` para `SQL_ORACLE` ou `SQL_SQLSERVER`, o mesmo payload sai com `"usuarios"`/`[usuarios]` (quoting de identificador) e `1`/`0` no lugar de `TRUE`/`FALSE` (nenhum dos dois tem tipo boolean nativo). Um `INSERT` por linha sempre — nunca `VALUES (...), (...)` numa instrução só, porque Oracle não suporta multi-row `VALUES`.

---

## 🔀 Motor de Transformação (`TransformationEngine`)

Além de converter formato, o `omnishift-core` já sabe reformatar a estrutura dos dados via `FieldMapping` (`sourcePath` → `targetPath`, com suporte a caminhos aninhados e índice de array na origem, ex: `itens[0].nome`). O resultado contém exclusivamente os campos mapeados (allow-list).

### Perfis de mapeamento nomeados

A API continua **agnóstica a dados**: o corpo da requisição nunca é envelopado para carregar instruções de mapeamento. Em vez disso, o cliente referencia um **perfil de mapeamento** pré-configurado no servidor:

* **REST**: header opcional `X-Mapping-Profile: nome-do-perfil`.
* **gRPC**: campo opcional `mapping_profile` na mensagem `ShiftGrpcRequest`.

Os perfis são definidos em `application.yml` (módulo `omnishift-runtime-spring`):
```yaml
omnishift:
  mapping-profiles:
    perfil-cliente-legado:
      - source-path: nome
        target-path: fullName
      - source-path: endereco.cidade
        target-path: address.city
```

Sem o header/campo, o comportamento é o mesmo de sempre: só conversão de formato, sem reformatação. Se o perfil citado não existir, a API responde `400 Bad Request` (REST) ou `INVALID_ARGUMENT` (gRPC).

**Trade-off assumido**: mapeamento não é ad-hoc por requisição — precisa estar pré-cadastrado no servidor. Em troca, sistemas heterogêneos só precisam conhecer o nome do perfil, nunca a sintaxe do DSL de mapeamento do OmniShift.

---

## ⚡ Streaming (REST)

O `/api/v1/shift` lê o corpo da requisição e escreve a resposta diretamente como fluxo de bytes (`InputStream`/`OutputStream`), em vez de materializar o payload inteiro como `String` em memória antes de processar — o mesmo contrato HTTP de sempre (headers, corpo cru), só que sem o gargalo de memória em payloads grandes. O limite de tamanho (`PayloadValidator`) é aplicado durante a leitura do stream, não sobre uma string já pronta, então funciona mesmo com `Transfer-Encoding: chunked`.

**Limites conhecidos, documentados de propósito**:
* A árvore `OmniNode` ainda é totalmente montada em memória entre o parse e a serialização — a transformação/mapeamento atua sobre a árvore completa, não campo a campo em streaming real. O ganho aqui é eliminar as cópias extras em `String` de cada lado, não reescrever o motor para processamento incremental.

---

## ⚡ Streaming (gRPC)

A RPC unária `ShiftData` continua exatamente como está (payloads pequenos, clientes simples). Para payloads grandes existe agora `ShiftDataStream`, uma RPC **bidirecional**: o cliente envia o payload em pedaços, o servidor devolve o resultado convertido também em pedaços. As duas RPCs convivem — nenhuma quebra a outra.

**Motivo de existir**: o gRPC tem um limite padrão de ~4MB por mensagem. Como `ShiftGrpcRequest`/`ShiftGrpcResponse` carregam o payload inteiro num único campo `string`, qualquer payload acima de ~4MB **falha categoricamente** hoje via `ShiftData`, independente de heap disponível. `ShiftDataStream` existe para contornar esse limite de tamanho de mensagem — não é streaming incremental campo-a-campo (mesmo limite de `OmniNode` em memória do REST, acima).

**Contrato de `ShiftDataStream`**:
1. A **primeira** mensagem enviada pelo cliente deve ser `ShiftGrpcRequestChunk.metadata` (`source_format`/`target_format`/`mapping_profile` opcional) — igual aos headers/campos das outras APIs.
2. As mensagens seguintes devem ser `ShiftGrpcRequestChunk.data_chunk`, com pedaços do payload bruto, em qualquer tamanho (recomendado ~256KB–1MB por pedaço, para ficar confortavelmente abaixo do limite padrão de ~4MB/mensagem do gRPC).
3. Ao fechar o stream de envio, o servidor processa o payload acumulado e devolve o resultado convertido como um ou mais `ShiftGrpcResponseChunk.data_chunk` (pedaços de 256KB), seguido do fechamento do stream de resposta.
4. Erros de contrato (pedaço de dado antes da metadata, metadata duplicada, nenhuma metadata enviada, formato inválido, perfil inexistente) encerram o stream com `INVALID_ARGUMENT`, igual à RPC unária.

**Limite de tamanho**: o payload total do streaming pode chegar a 50MB (bem acima do 1MB da RPC unária/REST), ainda finito porque o `OmniNode` é montado inteiro em memória antes de serializar.

---

## 📊 Observabilidade

O runtime expõe o [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html) com [Micrometer](https://micrometer.io/), em `/actuator/health`, `/actuator/metrics` e `/actuator/prometheus` (porta 8080, junto com o REST).

**Métrica principal**: `omnishift.conversions` (um `Timer`, não um `Counter` separado — a mesma convenção do `http.server.requests` do próprio Spring) dá contagem e latência juntos:
* `omnishift_conversions_seconds_count` / `..._sum` no formato Prometheus.
* Tags: `source_format`, `target_format`, `outcome` (`success`/`error`).

Gravada nos três canais de entrada (REST, gRPC unário, gRPC streaming) no ponto onde cada um já sabe formato de origem/destino e sucesso-ou-falha — não dentro de `ShiftDataUseCase` (que ficaria com uma métrica cruzando canais com formatos de chamada diferentes só para carregar uma tag).

**Limite de escopo, documentado de propósito**: para REST e `ShiftDataStream`, a métrica cobre parse+transform+resolução do serializer — não inclui a escrita final do stream de saída (que não pode falhar por causa de input do usuário, por design das Fases 6/7). Para a RPC unária `ShiftData`, cobre a conversão completa, porque ali é tudo um método só.

A porta de observabilidade (`MetricsRecorder`, em `omnishift-core/domain/ports`) segue o mesmo padrão já usado para `MappingProfileRepository`: interface livre de framework no core, implementação real (`MicrometerMetricsRecorder`) em `omnishift-runtime-spring`.

**Nota de correção**: `application.yml` tinha `server.port: 9090`, o que colidia com a porta padrão do próprio gRPC (também 9090, já que `grpc.server.port` nunca tinha sido definido explicitamente) — REST e gRPC disputavam a mesma porta na inicialização. Corrigido nesta fase: REST volta para 8080 (default do Spring Boot), gRPC fica explícito em 9090.

---

## 🛡️ Segurança e Tratamento de Erros
A API conta com um GlobalExceptionHandler configurado para mascarar rastros de infraestrutura interna (evitando stack traces na resposta), além de proteção contra ataques de Reflected XSS e Log Forging na camada de roteamento REST.

### Sanitização de conteúdo

* **Limite de tamanho**: `PayloadValidator` rejeita (`400 Bad Request` / `INVALID_ARGUMENT`) qualquer payload acima de 1.000.000 caracteres, tanto no REST quanto no gRPC, antes de qualquer parsing.
* **XXE (XML External Entity)**: o adapter XML desabilita DTD por completo no `XMLInputFactory` usado pelo Jackson — bloqueia tanto entidades externas (leitura de arquivos locais/SSRF) quanto expansão de entidade interna ("billion laughs").
* **Profundidade de aninhamento**: os três adapters (JSON/XML/YAML) configuram `StreamReadConstraints` com limite explícito de 500 níveis, para não depender do default implícito do Jackson e evitar `StackOverflowError` em payloads profundamente aninhados.
* **"YAML bomb" (expansão de alias/anchor)**: verificado empiricamente (testes em `JacksonYamlParserTest`) que o parser YAML do Jackson usado aqui é baseado em eventos, sem a fase de "compose" completa do SnakeYAML — `&ancora`, `*alias` e merge keys (`<<`) chegam como texto literal (o nome da âncora), nunca são expandidos para a estrutura referenciada. Não há, portanto, superfície para o ataque clássico de expansão exponencial via aliases neste adapter; a suspeita anterior de que isso precisaria de `LoaderOptions.setMaxAliasesForCollections` não se confirmou ao testar contra o parser real.
* **CSV Injection (Formula Injection)**: `CsvSerializer` prefixa com `'` qualquer célula cujo primeiro caractere seja `=`, `+`, `-`, `@`, TAB ou CR (recomendação da OWASP Cheat Sheet Series) — sem essa mitigação, uma célula desses arquivos poderia ser interpretada como fórmula pelo Excel/Google Sheets ao abrir o CSV exportado, um vetor real de RCE/exfiltração. Testado com um payload malicioso de verdade (`=cmd|'/c calc'!A1`).
* **SQL Injection**: gerar texto SQL a partir de dado não confiável é o cenário clássico de injeção se o escaping estiver errado. `SqlInsertSerializer` escapa todo valor string dobrando aspas simples (`'` → `''`, padrão ANSI que funciona nos 4 dialetos — não usa barra invertida, que é específico do MySQL e quebra sob `NO_BACKSLASH_ESCAPES`/`ANSI_QUOTES`) e todo identificador (tabela/coluna) dobrando o caractere de quoting do próprio dialeto. Números nunca vêm de string bruta do cliente quando a origem tem tipo (JSON/XML/YAML); quando a origem é CSV (sem inferência de tipo), um valor "numérico" passa pelo caminho de string entre aspas — mais verboso, mas seguro por construção. Testado com payloads maliciosos reais em valor (`x'); DROP TABLE usuarios; --`) e em nome de coluna, verificando a string SQL exata gerada.

---

 
*Desenvolvido com excelência técnica para suportar ecossistemas distribuídos e escaláveis.*


<br/>

<div align="center">
  <b>OmniShift</b> é uma fundação estrutural da <b>ZenithCode</b>.<br/>
  Liderado pela divisão <b>P.O.N.T.E</b> e arquitetado por <i>Gustavo "Tavera" Ventura</i>.

<br/><br/>

[![Arquitetura Limpa](https://img.shields.io/badge/Design-Clean_Architecture-blue)](#)
[![Java 21](https://img.shields.io/badge/Powered_by-Java_21-orange)](#)
[![Status](https://img.shields.io/badge/Status-Alpha_v1-success)](#)

  <br/>

  <sub> Construindo o amanhã, uma integração por vez. 🚀 </sub>
</div>
