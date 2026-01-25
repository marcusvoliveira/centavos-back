# Incentive - Sistema de Doações (Backend API)

API REST desenvolvida com Quarkus para gerenciar doações baseadas em extratos bancários.

**Este é um projeto backend puro.** O frontend React deve ser desenvolvido separadamente e consumir estas APIs.

## Tecnologias

- **Java 21** (LTS)
- **Quarkus 3.30.6**
- **PostgreSQL**
- **Panache** (Hibernate ORM)
- **SmallRye JWT** (Autenticação)
- **Flyway** (Migrations)
- **Quarkus Mailer** (Envio de emails)
- **Quarkus Scheduler** (Tarefas agendadas)

## Funcionalidades

### Autenticação e Segurança
- ✅ Login com JWT (Token)
- ✅ Controle de Roles (USER, ADMIN, MODERATOR)
- ✅ Verificação de email com código de 6 dígitos
- ✅ Senha criptografada com BCrypt

### Gestão de Dados
- ✅ Cadastro de usuários (nome, email, telefone)
- ✅ Gerenciamento de endereços
- ✅ Armazenamento de extratos bancários/cartão de crédito
- ✅ Registro de doações

### Automações
- ✅ Cálculo automático de doações baseado em extratos
- ✅ Envio de email semanal (toda segunda às 10h)
- ✅ Envio de email mensal (dia 5 às 10h)
- ✅ Cálculo mensal automático (dia 1 às 9h)

## Pré-requisitos

- Java 21
- Maven 3.8+
- PostgreSQL 12+
- (Opcional) Docker para rodar PostgreSQL

## Configuração

### 1. Banco de Dados

#### Opção A: PostgreSQL Local

Instale o PostgreSQL e crie o banco:

```bash
createdb incentive_db
```

#### Opção B: PostgreSQL com Docker

```bash
docker run -d \
  --name incentive-postgres \
  -e POSTGRES_DB=incentive_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:15
```

#### Opção C: Neon (Free Tier - 3GB)

1. Acesse [Neon](https://neon.tech)
2. Crie um projeto gratuito
3. Copie a connection string
4. Atualize `application.properties`:

```properties
quarkus.datasource.jdbc.url=sua-connection-string-neon
quarkus.datasource.username=seu-usuario
quarkus.datasource.password=sua-senha
```

### 2. Configurar Email (Gmail)

Edite `src/main/resources/application.properties`:

```properties
quarkus.mailer.username=seu-email@gmail.com
quarkus.mailer.password=sua-app-password
```

**Como gerar App Password do Gmail:**
1. Acesse [myaccount.google.com/security](https://myaccount.google.com/security)
2. Ative a verificação em 2 etapas
3. Vá em "Senhas de app"
4. Gere uma senha para "Email"
5. Use essa senha no `application.properties`

### 3. Executar o Projeto

#### Modo Desenvolvimento (com hot reload)

```bash
./mvnw quarkus:dev
```

O projeto estará disponível em: `http://localhost:8080`

#### Build e Execução

```bash
# Build
./mvnw clean package

# Executar
java -jar target/quarkus-app/quarkus-run.jar
```

#### Build Nativo (opcional)

```bash
./mvnw package -Pnative
```

## Endpoints da API

### Autenticação (públicos)

```bash
# Registrar usuário
POST /api/auth/register
{
  "name": "João Silva",
  "email": "joao@email.com",
  "password": "senha123",
  "phone": "11999999999"
}

# Login
POST /api/auth/login
{
  "email": "joao@email.com",
  "password": "senha123"
}

# Verificar email
POST /api/auth/verify-email
{
  "email": "joao@email.com",
  "code": "123456"
}

# Reenviar código
POST /api/auth/resend-verification?email=joao@email.com
```

### Usuários (autenticado)

```bash
# Obter dados do usuário atual
GET /api/users/me
Authorization: Bearer {token}

# Atualizar dados
PUT /api/users/me
Authorization: Bearer {token}
{
  "name": "João Silva Santos",
  "phone": "11888888888"
}

# Listar todos (ADMIN)
GET /api/users
Authorization: Bearer {token}
```

### Extratos Bancários (autenticado)

```bash
# Listar meus extratos
GET /api/bank-statements
Authorization: Bearer {token}

# Listar por período
GET /api/bank-statements?startDate=2025-01-01&endDate=2025-01-31
Authorization: Bearer {token}

# Criar extrato
POST /api/bank-statements
Authorization: Bearer {token}
{
  "accountType": "CREDIT_CARD",
  "accountDescription": "Nubank",
  "transactionDate": "2025-01-15",
  "description": "Compra Supermercado",
  "amount": 250.00,
  "transactionType": "DEBIT",
  "category": "Alimentação"
}

# Deletar extrato
DELETE /api/bank-statements/{id}
Authorization: Bearer {token}
```

### Doações (autenticado)

```bash
# Listar minhas doações
GET /api/donations
Authorization: Bearer {token}

# Calcular doação (1% padrão)
POST /api/donations/calculate?startDate=2025-01-01&endDate=2025-01-31
Authorization: Bearer {token}

# Calcular com percentual customizado (2%)
POST /api/donations/calculate?startDate=2025-01-01&endDate=2025-01-31&percentage=2
Authorization: Bearer {token}

# Ver total de doações
GET /api/donations/total
Authorization: Bearer {token}

# Processar doação (ADMIN)
POST /api/donations/{id}/process
Authorization: Bearer {token}
```

## Documentação da API

Acesse a documentação Swagger em: `http://localhost:8080/swagger-ui`

OpenAPI spec: `http://localhost:8080/openapi`

## Integração com Frontend React

### Configuração CORS

O backend já está configurado para aceitar requisições do frontend React. Edite `application.properties` para adicionar a URL do seu frontend:

```properties
# Desenvolvimento
quarkus.http.cors.origins=http://localhost:5173,http://localhost:3000

# Produção (adicione seu domínio)
quarkus.http.cors.origins=https://seu-frontend.vercel.app,https://seu-dominio.com
```

### Exemplo de Consumo no React

```javascript
// api.js - Configuração base
const API_URL = 'http://localhost:8080/api';

export const api = {
  // Login
  login: async (email, password) => {
    const response = await fetch(`${API_URL}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password })
    });
    return response.json();
  },

  // Requisições autenticadas
  getMe: async (token) => {
    const response = await fetch(`${API_URL}/users/me`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    return response.json();
  },

  // Criar extrato
  createStatement: async (token, data) => {
    const response = await fetch(`${API_URL}/bank-statements`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify(data)
    });
    return response.json();
  }
};
```

### Gestão de Token JWT

Armazene o token retornado no login:

```javascript
// Salvar token
localStorage.setItem('token', response.token);

// Usar token
const token = localStorage.getItem('token');

// Remover token (logout)
localStorage.removeItem('token');
```

### Variáveis de Ambiente (React)

```bash
# .env.local
VITE_API_URL=http://localhost:8080/api
# ou para Create React App
REACT_APP_API_URL=http://localhost:8080/api
```

## Estrutura do Projeto

```
src/main/java/org/example/incentive/
├── config/          # Configurações
├── dto/             # Data Transfer Objects
├── entity/          # Entidades JPA com Panache
├── resource/        # Endpoints REST
├── security/        # JWT e segurança
├── service/         # Lógica de negócio
└── util/            # Utilitários

src/main/resources/
├── application.properties
├── db/migration/    # Migrations Flyway
└── META-INF/
    └── resources/   # Chaves JWT
```

## Tarefas Agendadas

### Cálculo Mensal de Doações
- **Quando:** Todo dia 1 às 9h
- **O que faz:** Calcula doações do mês anterior para todos os usuários
- **Regra:** 1% dos gastos (débitos) do mês

### Relatório Semanal
- **Quando:** Toda segunda-feira às 10h
- **O que faz:** Envia email com resumo semanal de doações

### Relatório Mensal
- **Quando:** Todo dia 5 às 10h
- **O que faz:** Envia email com extrato completo do mês

## Credenciais Padrão

Usuário Admin criado automaticamente:
- **Email:** admin@incentive.com
- **Senha:** admin123
- **Role:** ADMIN

## Boas Práticas de Segurança

### Para Dados Bancários:

1. **Sempre use HTTPS em produção**
2. **Configure CORS adequadamente:**
   ```properties
   quarkus.http.cors.origins=https://seu-dominio.com
   ```
3. **Ative rate limiting (adicione extensão):**
   ```xml
   <dependency>
     <groupId>io.quarkus</groupId>
     <artifactId>quarkus-redis-client</artifactId>
   </dependency>
   ```

4. **Rotacione chaves JWT periodicamente**
5. **Implemente auditoria de acessos:**
   - Já há campos `created_at` e `updated_at`
   - Considere adicionar tabela de audit logs

### Conformidade LGPD/GDPR:

- Implemente endpoint para exportação de dados
- Implemente endpoint para exclusão de dados
- Adicione consent tracking
- Criptografe dados sensíveis no banco

## Deploy

### Render (Free Tier)

1. Crie conta no [Render](https://render.com)
2. Crie PostgreSQL database
3. Crie Web Service
4. Configure variáveis de ambiente
5. Deploy automático via Git

### Railway (Free Tier)

1. Crie conta no [Railway](https://railway.app)
2. Crie novo projeto do GitHub
3. Adicione PostgreSQL
4. Configure variáveis
5. Deploy automático

### Variáveis de Ambiente (produção)

```bash
QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://host/db
QUARKUS_DATASOURCE_USERNAME=user
QUARKUS_DATASOURCE_PASSWORD=pass
QUARKUS_MAILER_USERNAME=email@gmail.com
QUARKUS_MAILER_PASSWORD=app-password
QUARKUS_HTTP_CORS_ORIGINS=https://seu-frontend.com
```

## Desenvolvimento

### Executar testes

```bash
./mvnw test
```

### Verificar cobertura

```bash
./mvnw verify
```

### Lint e formatação

```bash
./mvnw spotless:apply
```

## Roadmap

- [ ] Adicionar suporte a múltiplas contas bancárias por usuário
- [ ] Implementar categorização automática de gastos
- [ ] Dashboard com gráficos de doações
- [ ] Integração com APIs bancárias (Open Banking)
- [ ] Sistema de metas de doação
- [ ] Notificações push
- [ ] App mobile (React Native / Flutter)

## Licença

MIT

## Suporte

Para questões ou suporte, abra uma issue no repositório.
