# OmniShift (motor de Orquestração de dados) 🔄

> **Universal Data Transformation Engine**
> Uma iniciativa [ZenithCode](https://github.com/gtvnv) mantida pela divisão P.O.N.T.E.

O OmniShift é um middleware de alta performance desenhado para orquestrar e transformar dados entre diferentes sistemas e formatos de forma agnóstica. Construído sob os princípios da **Arquitetura Hexagonal (Ports and Adapters)**, ele garante isolamento absoluto das regras de negócio, permitindo plugar novas tecnologias de entrada e saída sem atrito.

---

## 🏗️ Arquitetura

O coração do OmniShift é o seu **Modelo Canônico (`OmniNode`)**. Ao invés de converter um formato diretamente para outro (ex: JSON para XML), o sistema traduz qualquer formato de entrada para uma árvore de objetos em memória, e depois serializa essa árvore para o formato de saída desejado.

* **Inbound Adapters (Entrada):** REST (Spring Web), gRPC.
* **Outbound Adapters (Saída):** Jackson JSON, Jackson XML.
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
* O **gRPC continua não-streaming**: a RPC `ShiftData` é unária, e o próprio protocolo já buffereia a mensagem inteira antes de entregá-la ao handler. Streaming de verdade ali exigiria mudar o contrato `.proto` para uma RPC de streaming — fica como trabalho futuro.

---

## 🛡️ Segurança e Tratamento de Erros
A API conta com um GlobalExceptionHandler configurado para mascarar rastros de infraestrutura interna (evitando stack traces na resposta), além de proteção contra ataques de Reflected XSS e Log Forging na camada de roteamento REST.

### Sanitização de conteúdo

* **Limite de tamanho**: `PayloadValidator` rejeita (`400 Bad Request` / `INVALID_ARGUMENT`) qualquer payload acima de 1.000.000 caracteres, tanto no REST quanto no gRPC, antes de qualquer parsing.
* **XXE (XML External Entity)**: o adapter XML desabilita DTD por completo no `XMLInputFactory` usado pelo Jackson — bloqueia tanto entidades externas (leitura de arquivos locais/SSRF) quanto expansão de entidade interna ("billion laughs").
* **Profundidade de aninhamento**: os três adapters (JSON/XML/YAML) configuram `StreamReadConstraints` com limite explícito de 500 níveis, para não depender do default implícito do Jackson e evitar `StackOverflowError` em payloads profundamente aninhados.
* **Follow-up conhecido, não implementado**: o SnakeYAML por trás do adapter YAML tem proteção própria contra expansão de alias/anchor ("YAML bomb", `LoaderOptions.setMaxAliasesForCollections`) que ainda não foi configurada explicitamente aqui.

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
