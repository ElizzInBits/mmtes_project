# HoraCerta 

Sistema de **agendamento automatizado via WhatsApp** para pequenas empresas. Um chatbot inteligente conversa com seus clientes, consulta sua agenda do Google Calendar em tempo real e realiza agendamentos de forma totalmente autônoma, sem precisar de atendimento humano para etapas como agendamento, consulta de catálogo e solicitação de orçamento.

O template de conversa do backend pode ser personalizado conforme o objetivo da empresa, e toda a integração com o WhatsApp é feita através da **Evolution API**.

---

## Sumário

- [HoraCerta](#horacerta)
  - [Sumário](#sumário)
  - [Como o sistema funciona](#como-o-sistema-funciona)
  - [Arquitetura e serviços](#arquitetura-e-serviços)
  - [Pré-requisitos](#pré-requisitos)
  - [Configuração do Google Cloud](#configuração-do-google-cloud)
    - [1. Criar um projeto no Google Cloud](#1-criar-um-projeto-no-google-cloud)
    - [2. Ativar a API do Google Calendar](#2-ativar-a-api-do-google-calendar)
    - [3. Criar a Service Account](#3-criar-a-service-account)
    - [4. Baixar o arquivo de credenciais JSON](#4-baixar-o-arquivo-de-credenciais-json)
    - [5. Compartilhar a agenda com a Service Account](#5-compartilhar-a-agenda-com-a-service-account)
    - [6. Copiar o arquivo de credenciais para o projeto](#6-copiar-o-arquivo-de-credenciais-para-o-projeto)
  - [Clonando o repositório](#clonando-o-repositório)
    - [1. Clone este repositório](#1-clone-este-repositório)
    - [2. Clone a Evolution API](#2-clone-a-evolution-api)
  - [Configurando as variáveis de ambiente](#configurando-as-variáveis-de-ambiente)
    - [1. Backend : `application.properties`](#1-backend--applicationproperties)
    - [2. Evolution API : `.env`](#2-evolution-api--env)
  - [Executando com Docker Compose](#executando-com-docker-compose)
  - [Conectando o WhatsApp](#conectando-o-whatsapp)
    - [Opção 1: Painel Evolution Manager](#opção-1-painel-evolution-manager)
    - [Opção 2: Swagger do Backend](#opção-2-swagger-do-backend)
  - [Executando o backend em modo de desenvolvimento](#executando-o-backend-em-modo-de-desenvolvimento)
    - [Requisitos para modo de desenvolvimento](#requisitos-para-modo-de-desenvolvimento)
    - [Iniciando o backend](#iniciando-o-backend)
    - [Rodando os testes](#rodando-os-testes)
  - [Estrutura do projeto](#estrutura-do-projeto)
  - [Fluxo de conversa do chatbot](#fluxo-de-conversa-do-chatbot)
    - [Comandos disponíveis durante a conversa](#comandos-disponíveis-durante-a-conversa)
    - [Opções do menu principal](#opções-do-menu-principal)
  - [Endpoints disponíveis](#endpoints-disponíveis)
    - [Swagger UI](#swagger-ui)
    - [WhatsApp Connection — `/api/v1/whatsapp`](#whatsapp-connection--apiv1whatsapp)
    - [Webhook: `/api/v1/webhook`](#webhook-apiv1webhook)
  - [Observações importantes](#observações-importantes)

---

## Como o sistema funciona

```text
Cliente envia mensagem no WhatsApp
           ↓
    Evolution API recebe
    e repassa ao backend
           ↓
    Backend processa a
    mensagem e guia o
    cliente pelo fluxo
           ↓
    Google Calendar é
    consultado e o evento
    é criado ao confirmar
           ↓
    Resposta enviada de
    volta ao cliente via
    Evolution API
```

O cliente ativa o chatbot enviando a palavra-chave `/teste`. Essa palavra-chave pode ser removida para um caso real, onde o chatbot irá responder qualquer mensagem inicial enviada pelo cliente. A partir daí, o bot conduz a conversa por um menu interativo até concluir o agendamento na agenda do Google.

---

## Arquitetura e serviços

O sistema é composto por **5 serviços** orquestrados via Docker Compose. O arquivo `docker-compose.yml` define como cada serviço é iniciado, como eles se comunicam entre si (via rede interna `horacerta-net`) e como os dados são persistidos (via volumes Docker).

| Serviço              | Porta  | Descrição                                                                 |
| -------------------- | ------ | ------------------------------------------------------------------------- |
| **horacerta-backend**   | `8081` | API Java Spring Boot, é cérebro do chatbot, compilada a partir do código   |
| **evolution-api**       | `8082` | Gateway WhatsApp, baseado na [Evolution API](https://github.com/EvolutionAPI/evolution-api) |
| **evolution-manager**   | `8082/manager` | Painel web para gerenciar instâncias e conexões WhatsApp                  |
| **evolution-redis**     | `6379` | Cache interno da Evolution API (não exposto externamente)                 |
| **evolution-postgres**  | `5432` | Banco de dados interno da Evolution API (não exposto externamente)        |

> O backend se comunica diretamente apenas com a **Evolution API** e com o **Google Calendar**. Redis e Postgres são infraestrutura interna da Evolution o backend não sabe que eles existem.

Dentro do Docker Compose, os serviços se comunicam pelo **nome do serviço** como endereço. Por exemplo, o backend acessa a Evolution pelo endereço `http://evolution-api:8080`, e o Docker resolve esse nome automaticamente.

---

## Pré-requisitos

Antes de começar, certifique-se de ter instalado:

- [Git](https://git-scm.com/)
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (Windows/Mac) ou Docker Engine + Docker Compose (Linux)
- [Java 21+](https://adoptium.net/), apenas para rodar o backend em modo de desenvolvimento
- [Maven 3.8+](https://maven.apache.org/), apenas para rodar o backend em modo de desenvolvimento
- Uma conta Google com acesso ao [Google Cloud Console](https://console.cloud.google.com/)

---

## Configuração do Google Cloud

O backend precisa de uma **Service Account** do Google para acessar o Google Calendar. Siga os passos abaixo:

### 1. Criar um projeto no Google Cloud

1. Acesse [console.cloud.google.com](https://console.cloud.google.com/)
2. Clique em **Selecionar projeto** > **Novo projeto**
3. Dê um nome ao projeto (ex.: `horacerta`) e clique em **Criar**

### 2. Ativar a API do Google Calendar

1. No menu lateral, vá em **APIs e serviços** > **Biblioteca**
2. Pesquise por `Google Calendar API`
3. Clique no resultado e depois em **Ativar**

### 3. Criar a Service Account

1. Vá em **APIs e serviços** > **Credenciais**
2. Clique em **+ Criar credenciais** > **Conta de serviço**
3. Preencha o nome (ex.: `horacerta-calendar`) e clique em **Criar e continuar**
4. Em "Conceder acesso", selecione a função **Editor** e clique em **Continuar** > **Concluído**

### 4. Baixar o arquivo de credenciais JSON

1. Na lista de contas de serviço, clique na que você acabou de criar
2. Vá na aba **Chaves** > **Adicionar chave** > **Criar nova chave**
3. Selecione o formato **JSON** e clique em **Criar**
4. O arquivo será baixado automaticamente, guarde-o em segurança, pois contém credenciais de acesso à sua conta

### 5. Compartilhar a agenda com a Service Account

1. Abra o [Google Calendar](https://calendar.google.com/)
2. No calendário que deseja usar, clique nos três pontos > **Configurações e compartilhamento**
3. Em **Compartilhar com pessoas específicas**, clique em **+ Adicionar pessoas**
4. Cole o e-mail da service account (termina com `@...iam.gserviceaccount.com`)
5. Defina a permissão como **Fazer alterações nos eventos** e salve
6. Anote o **ID do calendário**, disponível na seção "Integrar agenda" das configurações (formato: `seu-email@gmail.com` ou `abc123@group.calendar.google.com`)

### 6. Copiar o arquivo de credenciais para o projeto

Renomeie o arquivo JSON baixado para `google-credentials.json` e coloque-o em:

```text
HoraCerta/backend/src/main/resources/google-credentials.json
```

---

## Clonando o repositório

### 1. Clone este repositório

```bash
git clone https://github.com/ElizzInBits/mmtes_project
cd mmtes_project/HoraCerta
```

### 2. Clone a Evolution API

A Evolution API é um projeto de terceiros mantido pela equipe da [EvolutionAPI](https://github.com/EvolutionAPI/evolution-api) e **não está incluída neste repositório**. Você precisa cloná-la manualmente dentro da estrutura de pastas correta.

```bash
# Dentro de HoraCerta/, crie a pasta evolution e entre nela
mkdir -p evolution
cd evolution

# Clone a Evolution API
git clone https://github.com/EvolutionAPI/evolution-api
```

Após o clone, a estrutura de pastas deve ficar exatamente assim:

```text
HoraCerta/
├── backend/
├── docker-compose.yml
└── evolution/
    └── evolution-api/       ← pasta clonada da Evolution API
        ├── .env.example
        ├── Dockerfile
        └── ...
```

> O `docker-compose.yml` espera encontrar o arquivo `.env` da Evolution em `./evolution/evolution-api/.env`. Se a pasta estiver em outro lugar, o Docker Compose não conseguirá subir o serviço `evolution-api`.

---

## Configurando as variáveis de ambiente

### 1. Backend : `application.properties`

Copie o arquivo de exemplo e preencha com seus dados:

```bash
cp backend/src/main/resources/application.properties.example \
   backend/src/main/resources/application.properties
```

Edite o arquivo `application.properties`:

```properties
spring.application.name=horacerta-backend
server.port=8081

# URL da Evolution API
# Se rodar pelo Docker Compose, use o nome do serviço: http://evolution-api:8080
# Se rodar o backend fora do Docker (modo dev), use: http://localhost:8082
evolution.api.url=http://localhost:8082

# Token de autenticação da Evolution API
# Deve ser o mesmo valor definido em AUTHENTICATION_API_KEY no .env da Evolution
evolution.api.token=SEU_TOKEN_AQUI

# Nome da instância WhatsApp que será criada
evolution.api.instance=horacerta

# ID do calendário Google (e-mail da conta ou ID do calendário anotado no passo 5)
google.calendar.id=seu-email@gmail.com

# Palavra-chave que ativa o chatbot para novos contatos
# Contatos sem sessão ativa só são atendidos se enviarem exatamente essa palavra
# Para um ambiente de produção, remova a restrição no WebhookController
chatbot.trigger.keyword=/teste
```

### 2. Evolution API : `.env`

O repositório da Evolution API já vem com um arquivo `.env` contendo todas as configurações básicas necessárias. Basta copiá-lo e ajustar apenas o `SERVER_URL` para o endereço onde a API será acessada:

```bash
evolution/evolution-api/.env
```

Abra o `.env` e altere apenas essa linha:

```env
# Substitua pelo endereço onde a Evolution API será acessada
# Para testes locais:
SERVER_URL=http://localhost:8082

# Para produção (com domínio próprio):
# SERVER_URL=https://seu-dominio.com
```

> Todas as outras configurações do `.env`, incluindo banco de dados, Redis, token de autenticação e webhook, já vêm com valores padrão prontos para uso com o `docker-compose.yml` deste projeto. O único valor que você **pode** querer alterar é o `AUTHENTICATION_API_KEY`, caso queira usar um token personalizado. Se fizer isso, lembre de atualizar o mesmo valor em `evolution.api.token` no `application.properties` do backend.

---

## Executando com Docker Compose

Com o Docker Desktop aberto e rodando, execute dentro da pasta `HoraCerta/`:

```bash
cd HoraCerta
docker compose up -d
```

O Docker irá compilar o backend a partir do código-fonte e baixar as imagens dos demais serviços automaticamente. Aguarde todos os contêineres subirem.

Você pode acompanhar os logs com:

```bash
# Todos os serviços
docker compose logs -f

# Apenas o backend
docker compose logs -f horacerta-backend

# Apenas a Evolution API
docker compose logs -f evolution-api
```

Para verificar o status de todos os serviços:

```bash
docker compose ps
```

Todos devem estar com status `running`. Para parar tudo:

```bash
docker compose down
```

---

## Conectando o WhatsApp

Após subir os serviços, você precisa criar uma instância e escanear o QR Code para conectar o número de WhatsApp ao sistema. Existem duas formas de fazer isso:

---

### Opção 1: Painel Evolution Manager

1. Acesse <http://localhost:8082/manager> e utilize o token presente na env do evolution para realizar o login.
2. Clique em **New Instance**
3. Preencha o nome da instância com o mesmo valor definido em `evolution.api.instance` no `application.properties` (padrão: `horacerta`)
4. Após criar, clique na instância e depois em **Connect**
5. Um QR Code será exibido na tela escaneie com o WhatsApp do número que será usado como bot
6. Após a conexão, configure o webhook manualmente:
   - Vá em **Settings** da instância
   - Em **Webhook**, defina a URL: `http://horacerta-backend:8081/api/v1/webhook/evolution`
   - Ative o evento `MESSAGES_UPSERT`
   - Salve

> Como o Evolution Manager e o backend estão no mesmo Docker Compose, use o nome do serviço `horacerta-backend` como endereço. Se o backend estiver fora do Docker, use `http://host.docker.internal:8081/api/v1/webhook/evolution`.

---

### Opção 2: Swagger do Backend

O backend oferece endpoints que automatizam todo o processo de setup:

1. Acesse <http://localhost:8081/swagger-ui.html>
2. Expanda a seção **WhatsApp Connection**
3. Execute o endpoint `POST /api/v1/whatsapp/setup`, ele cria a instância na Evolution API (se não existir) e configura o webhook automaticamente apontando para o backend
4. Para obter o QR Code, acesse no navegador:

   ```text
   http://localhost:8081/api/v1/whatsapp/qrcode/horacerta
   ```

   A imagem do QR Code será exibida diretamente no navegador

5. Escaneie com o WhatsApp do número que será usado como bot

> Esta é a forma mais simples, um único endpoint configura tudo automaticamente.

---

## Executando o backend em modo de desenvolvimento

Para desenvolver sem precisar do Docker, você pode rodar o backend diretamente com Maven. Nesse caso, a Evolution API ainda precisa estar rodando (via Docker ou outro meio) e o `evolution.api.url` no `application.properties` deve apontar para `http://localhost:8082`.

### Requisitos para modo de desenvolvimento

- Java 21+
- Maven 3.8+
- Evolution API rodando e acessível
- `application.properties` configurado
- `google-credentials.json` presente em `src/main/resources/`

### Iniciando o backend

```bash
cd HoraCerta/backend
mvn spring-boot:run
```

O backend estará disponível em <http://localhost:8081>.

### Rodando os testes

```bash
cd HoraCerta/backend
mvn test
```

---

## Estrutura do projeto

```text
HoraCerta/
├── docker-compose.yml                   # Orquestração de todos os serviços
├── backend/                             # Backend Java Spring Boot
│   ├── src/main/java/com/horacerta/backend/
│   │   ├── application/usecase/         # Lógica de negócio e state handlers
│   │   ├── domain/
│   │   │   ├── model/                   # ChatSession, ChatState
│   │   │   └── port/out/                # Interfaces: MessagingPort, CalendarPort, etc.
│   │   └── infrastructure/adapter/
│   │       ├── in/web/                  # Controllers (WebhookController, WhatsAppController)
│   │       └── out/
│   │           ├── evolution/           # Cliente Feign para a Evolution API
│   │           ├── google/              # Integração com Google Calendar
│   │           └── persistence/         # Repositório em memória de sessões
│   └── src/main/resources/
│       ├── application.properties       # Configurações (não versionado)
│       └── google-credentials.json      # Credenciais Google (não versionado)
└── evolution/
    └── evolution-api/                   # ← clonar de github.com/EvolutionAPI/evolution-api
        └── .env                         # Variáveis de ambiente da Evolution 
```

---

## Fluxo de conversa do chatbot

```text
Cliente: /teste
Bot: "Olá, [Nome]! Bem-vindo ao HoraCerta.
      1. Ver Catálogo
      2. Orçamento
      3. Agendar"

Cliente: 3
Bot: Lista horários disponíveis nos próximos 7 dias
     (consultados em tempo real no Google Calendar)

Cliente: 2  (seleciona o segundo horário da lista)
Bot: "Horário selecionado! Digite seu NOME COMPLETO:"

Cliente: João da Silva
Bot: "Digite seu CPF:"

Cliente: 123.456.789-00
Bot: "Confirmando Agendamento...
      Cliente: João da Silva
      CPF: 123.456.789-00
      Data: 18/06/2026 às 10:00

      Posso confirmar? Digite SIM para finalizar."

Cliente: SIM
Bot: "Agendamento realizado com sucesso!"
       Evento criado no Google Calendar
```

### Comandos disponíveis durante a conversa

| Comando            | Ação                              |
| ------------------ | --------------------------------- |
| `sair` ou `cancelar` | Encerra a conversa imediatamente |
| `voltar`           | Retorna à etapa anterior          |

### Opções do menu principal

| Opção | Ação                              |
| ----- | --------------------------------- |
| `1`   | Exibe link do catálogo de serviços |
| `2`   | Abre fluxo de solicitação de orçamento |
| `3`   | Inicia o fluxo de agendamento     |

---

## Endpoints disponíveis

### Swagger UI

Documentação interativa completa com todos os endpoints:

```text
http://localhost:8081/swagger-ui.html
```

### WhatsApp Connection — `/api/v1/whatsapp`

| Método | Endpoint               | Descrição                                        |
| ------ | ---------------------- | ------------------------------------------------ |
| `POST` | `/setup`               | Cria instância e configura webhook automaticamente |
| `POST` | `/webhook`             | Configura apenas o webhook da instância          |
| `GET`  | `/qrcode/{instanceName}` | Retorna o QR Code como imagem PNG              |
| `GET`  | `/instances`           | Lista todas as instâncias criadas                |

### Webhook: `/api/v1/webhook`

| Método | Endpoint     | Descrição                                          |
| ------ | ------------ | -------------------------------------------------- |
| `POST` | `/evolution` | Recebe eventos da Evolution API (uso interno)      |

---

## Observações importantes

- **Sessões em memória**: as conversas em andamento ficam armazenadas na RAM do servidor. Se o backend reiniciar, conversas não concluídas são perdidas e os clientes precisam começar do zero.
- **Palavra-chave de ativação**: por padrão, o bot só responde a novos contatos se eles enviarem exatamente `/teste`. Isso evita que o bot interfira em conversas normais do número. Para remover essa restrição em produção, altere a lógica no `WebhookController`.
- **Horários disponíveis**: o bot consulta horários livres das 9h às 18h, em slots de 1 hora, nos próximos 7 dias. Horários já ocupados no Google Calendar são automaticamente excluídos da lista.
- **Segurança**: nunca compartilhe o arquivo `google-credentials.json` nem o `application.properties`, ambos estão no `.gitignore` por conterem credenciais sensíveis.
- **Token da Evolution API**: o valor de `AUTHENTICATION_API_KEY` no `.env` da Evolution e o `evolution.api.token` no `application.properties` do backend devem ser **idênticos**. Se alterar um, altere o outro.
