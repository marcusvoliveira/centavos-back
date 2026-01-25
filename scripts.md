# Scripts Úteis

## Desenvolvimento

### Iniciar banco de dados (Docker)
```bash
docker-compose up -d postgres
```

### Parar banco de dados
```bash
docker-compose down
```

### Resetar banco de dados
```bash
docker-compose down -v
docker-compose up -d postgres
```

### Executar aplicação em modo dev
```bash
./mvnw quarkus:dev
```

### Compilar projeto
```bash
./mvnw clean package
```

### Executar testes
```bash
./mvnw test
```

## Banco de Dados

### Conectar ao PostgreSQL (Docker)
```bash
docker exec -it incentive-postgres psql -U postgres -d incentive_db
```

### Ver tabelas
```sql
\dt
```

### Ver dados da tabela users
```sql
SELECT id, name, email, role, email_verified FROM users;
```

### Criar usuário admin manualmente
```sql
INSERT INTO users (name, email, password, role, email_verified, created_at, updated_at)
VALUES (
    'Admin User',
    'admin@example.com',
    '$2a$10$XH2YfYJNlZxPF1pLJJfW4uKk2Uw8OHUo5MJyqQNI9LqKQxPYf.Rw2',
    'ADMIN',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
```

## Testes de API (curl)

### Registrar usuário
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "João Silva",
    "email": "joao@email.com",
    "password": "senha123",
    "phone": "11999999999"
  }'
```

### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "joao@email.com",
    "password": "senha123"
  }'
```

### Obter dados do usuário (substituir TOKEN)
```bash
TOKEN="seu-token-aqui"

curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer $TOKEN"
```

### Criar extrato bancário
```bash
TOKEN="seu-token-aqui"

curl -X POST http://localhost:8080/api/bank-statements \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "accountType": "CREDIT_CARD",
    "accountDescription": "Nubank",
    "transactionDate": "2025-01-15",
    "description": "Compra Supermercado",
    "amount": 250.00,
    "transactionType": "DEBIT",
    "category": "Alimentação"
  }'
```

### Calcular doação
```bash
TOKEN="seu-token-aqui"

curl -X POST "http://localhost:8080/api/donations/calculate?startDate=2025-01-01&endDate=2025-01-31" \
  -H "Authorization: Bearer $TOKEN"
```

## Regenerar chaves JWT

```bash
cd src/main/resources/META-INF/resources

# Gerar chave privada RSA
openssl genrsa -out rsaPrivateKey.pem 2048

# Extrair chave pública
openssl rsa -pubout -in rsaPrivateKey.pem -out publicKey.pem

# Converter para PKCS8
openssl pkcs8 -topk8 -nocrypt -inform pem -in rsaPrivateKey.pem -outform pem -out privateKey.pem

# Remover arquivo temporário
rm rsaPrivateKey.pem
```

## Deploy

### Build para produção
```bash
./mvnw clean package -DskipTests
```

### Build nativo (requer GraalVM)
```bash
./mvnw package -Pnative -DskipTests
```

### Executar em produção
```bash
java -jar target/quarkus-app/quarkus-run.jar
```

## Monitoramento

### Ver logs
```bash
tail -f logs/application.log
```

### Health check
```bash
curl http://localhost:8080/q/health
```

### Metrics
```bash
curl http://localhost:8080/q/metrics
```

## Utilitários

### Gerar hash BCrypt para senha
```bash
# Instale bcrypt-cli
npm install -g bcrypt-cli

# Gerar hash
bcrypt-cli "sua-senha-aqui"
```

### Ver estrutura do projeto
```bash
tree -I 'target|node_modules|.idea' -L 4
```

### Limpar build
```bash
./mvnw clean
```
