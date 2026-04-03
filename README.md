# OmniShift 🔄

> **Universal Data Transformation Engine**
> Uma iniciativa [ZenithCode](https://github.com/gtvnv) mantida pela divisão P.O.N.T.E.

O OmniShift é um middleware de alta performance desenhado para orquestrar e transformar dados entre diferentes sistemas e formatos de forma agnóstica. Construído sob os princípios da **Arquitetura Hexagonal (Ports and Adapters)**, ele garante isolamento absoluto das regras de negócio, permitindo plugar novas tecnologias de entrada e saída sem atrito.

---

## 🏗️ Arquitetura

O coração do OmniShift é o seu **Modelo Canônico (`OmniNode`)**. Ao invés de converter um formato diretamente para outro (ex: JSON para XML), o sistema traduz qualquer formato de entrada para uma árvore de objetos em memória, e depois serializa essa árvore para o formato de saída desejado.

* **Inbound Adapters (Entrada):** REST (Spring Web), gRPC.
* **Outbound Adapters (Saída):** Jackson JSON, Jackson XML.
* **Core (Domínio):** Regras de conversão e orquestração totalmente independentes de frameworks externos.

---

## 🚀 Tecnologias Utilizadas

* **Java 21** (Utilizando *Pattern Matching* e *Records*)
* **Spring Boot 3.3**
* **gRPC & Protobuf** (Comunicação de alta performance)
* **Jackson** (Serialização de dados)
* **Maven**

---

## ⚙️ Como Executar (Setup Local)

### Pré-requisitos
* Java Development Kit (JDK) 21
* Maven 3.8+ instalado (ou utilize o Maven Wrapper `mvnw` incluso no projeto)

---

### Compilação
O projeto utiliza gRPC, portanto, uma compilação inicial é necessária para gerar as classes do Protobuf:
```bash
mvn clean compile
```

---

### Subindo a Aplicação
Execute o projeto via Maven:

```bash
mvn spring-boot:run
```

O servidor será iniciado na porta padrão 8080 para REST e 9090 para chamadas gRPC.

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

## 🛡️ Segurança e Tratamento de Erros
A API conta com um GlobalExceptionHandler configurado para mascarar rastros de infraestrutura interna (evitando stack traces na resposta), além de proteção contra ataques de Reflected XSS e Log Forging na camada de roteamento REST.

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