# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Incentive** is a multi-project backend REST API built with Quarkus that manages donation calculations based on bank statement transactions. The system supports multiple projects with role-based access control, automatically calculates 1% of user expenses as monthly donations, and provides scheduled email reports.

**Key Features:**
- **Multi-Project Architecture**: Users can belong to multiple projects with different roles
- **Role-Based Access Control**: USER, MODERATOR, AGENT, and ADMIN roles
- **Project Isolation**: Each project has its own users, bank statements, and donations
- **Flexible Permissions**: Moderators manage their projects, Agents have read-only access

**Technology Stack:**
- Java 21 (LTS)
- Quarkus 3.30.6
- PostgreSQL with Panache (Hibernate ORM)
- SmallRye JWT for authentication
- Flyway for database migrations
- Quarkus Mailer for email notifications
- Quarkus Scheduler for automated tasks

## Development Commands

### Start Development Server
```bash
./mvnw quarkus:dev
```
Runs the application with hot reload on `http://localhost:8080`

### Build
```bash
# Standard JAR build
./mvnw clean package

# Build without tests
./mvnw clean package -DskipTests

# Native build (requires GraalVM)
./mvnw package -Pnative
```

### Testing
```bash
# Run all tests
./mvnw test

# Run with verification
./mvnw verify
```

### Database Management
```bash
# Start PostgreSQL with Docker
docker-compose up -d postgres

# Connect to PostgreSQL
docker exec -it incentive-postgres psql -U postgres -d incentive_db

# Reset database (delete all data)
docker-compose down -v
docker-compose up -d postgres
```

### JWT Key Regeneration
When you need to regenerate JWT keys (located in `src/main/resources/META-INF/resources/`):
```bash
cd src/main/resources/META-INF/resources
openssl genrsa -out rsaPrivateKey.pem 2048
openssl rsa -pubout -in rsaPrivateKey.pem -out publicKey.pem
openssl pkcs8 -topk8 -nocrypt -inform pem -in rsaPrivateKey.pem -outform pem -out privateKey.pem
rm rsaPrivateKey.pem
```

## Architecture

### Package Structure
```
com.incentive/
├── dto/              - Data Transfer Objects for API requests/responses
├── entity/           - JPA entities using Panache Active Record pattern
├── resource/         - REST endpoints (controllers)
├── security/         - JWT token generation and validation
├── service/          - Business logic and scheduled tasks
└── util/            - Utility classes (password hashing, etc.)
```

### Key Entities

**User** (`entity/User.java`)
- Uses Panache Active Record pattern
- Fields: name, email, cpf, password (BCrypt hashed), phone, role, emailVerified
- Includes email verification with 6-digit code and expiration
- Custom finders: `findByEmail()`, `findByEmailAndVerificationCode()`, `existsByEmail()`

**BankStatement** (`entity/BankStatement.java`)
- Tracks financial transactions for users
- AccountType: CHECKING_ACCOUNT, SAVINGS_ACCOUNT, CREDIT_CARD
- TransactionType: DEBIT, CREDIT
- Custom finders: `findByUserId()`, `findByUserIdAndDateRange()`, `calculateTotalByUserAndDateRange()`

**Donation** (`entity/Donation.java`)
- Stores calculated donation amounts
- DonationStatus: PENDING, PROCESSED, CANCELLED
- Custom finders: `findByUserId()`, `findByUserIdAndStatus()`, `calculateTotalByUserId()`

**Project** (`entity/Project.java`)
- Represents a project in the system
- Fields: name, description, active
- Custom finders: `findActiveProjects()`, `findByName()`
- Each project can have multiple users with different roles

**UserProject** (`entity/UserProject.java`)
- Many-to-many relationship between User and Project
- Stores the user's role within a specific project
- Unique constraint: one user can have only one role per project
- Custom finders: `findByUserId()`, `findByProjectId()`, `findByUserAndProject()`, `userHasAccessToProject()`, `userIsModeratorOfProject()`

### Multi-Project Architecture

The system supports multiple independent projects, each with its own set of users and permissions:

1. **User-Project Association**: Users are associated with projects through the `UserProject` entity
2. **Project-Scoped Roles**: Each user can have different roles in different projects
3. **Access Control**:
   - **ADMIN**: Global access to all projects and full system administration
   - **MODERATOR**: Can manage their assigned projects (create, update, delete, manage users)
   - **AGENT**: Read-only access to assigned projects (can view but not modify)
   - **USER**: Basic access to assigned projects with standard user permissions

4. **Default Project**: Existing users are automatically assigned to a "Default Project" during migration

### Authentication & Security

**JWT Token Flow:**
1. User registers via `/api/auth/register` - creates unverified account
2. System sends 6-digit verification code to email
3. User verifies email via `/api/auth/verify-email`
4. User logs in via `/api/auth/login` - receives JWT token
5. Token must be included in `Authorization: Bearer <token>` header for protected endpoints

**Roles:** USER, ADMIN, MODERATOR, AGENT (defined in `entity/Role.java`)
- **USER**: Standard user with basic project access
- **MODERATOR**: Project owner/manager with full project permissions
- **AGENT**: Read-only project access (like MODERATOR but without configuration permissions)
- **ADMIN**: System-wide administrator with access to all projects

**TokenService** (`security/TokenService.java`) handles JWT generation with:
- RSA key pair signing
- Issuer: https://incentive.com
- Default expiration: 86400 seconds (1 day)
- Claims: user ID, email, name, role

### Scheduled Tasks

**DonationScheduler** (`service/DonationScheduler.java`) runs three automated jobs:

1. **Monthly Donation Calculation** - Every 1st of month at 9:00 AM
   - Calculates 1% of previous month's expenses (DEBIT transactions)
   - Creates Donation records for all verified users
   - Cron: `0 0 9 1 * ?`

2. **Weekly Report Email** - Every Monday at 10:00 AM
   - Sends HTML email with weekly donation summary
   - Cron: `0 0 10 ? * MON`

3. **Monthly Report Email** - Every 5th of month at 10:00 AM
   - Sends detailed monthly donation statement
   - Includes total processed donations
   - Cron: `0 0 10 5 * ?`

### Business Logic

**DonationCalculationService** (`service/DonationCalculationService.java`)
- Calculates donations based on user's bank statement DEBIT transactions
- Default percentage: 1% of expenses
- Supports custom percentage via API parameter
- Creates PENDING donation records

**EmailService** (`service/EmailService.java`)
- Sends verification codes (6-digit, 15-minute expiration)
- Sends statement reports (weekly/monthly)
- Uses Gmail SMTP (configured in application.properties)

## Configuration

### Database
Edit `src/main/resources/application.properties`:
```properties
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/incentive_db
quarkus.datasource.username=postgres
quarkus.datasource.password=postgres
```

### Email (Gmail)
```properties
quarkus.mailer.username=your-email@gmail.com
quarkus.mailer.password=your-app-password
```
Generate Gmail App Password at: https://myaccount.google.com/security

### CORS
```properties
quarkus.http.cors.origins=http://localhost:5173,http://localhost:3000
```
Add production frontend URLs when deploying.

## API Endpoints

### Authentication (Public)
- `POST /api/auth/register` - Create new user account
- `POST /api/auth/login` - Authenticate and receive JWT token (returns user data + list of projects)
- `POST /api/auth/verify-email` - Verify email with 6-digit code
- `POST /api/auth/resend-verification` - Request new verification code

### Users (Authenticated)
- `GET /api/users/me` - Get current user profile
- `PUT /api/users/me` - Update current user
- `GET /api/users` - List all users (ADMIN only)

### Projects (Authenticated)
- `GET /api/projects` - List user's projects (ADMIN sees all, others see only assigned projects)
- `GET /api/projects/{id}` - Get specific project details
- `POST /api/projects` - Create new project (ADMIN, MODERATOR)
- `PUT /api/projects/{id}` - Update project (ADMIN or project MODERATOR)
- `DELETE /api/projects/{id}` - Delete project (ADMIN or project MODERATOR)
- `GET /api/projects/{id}/users` - List users in project
- `POST /api/projects/{id}/users` - Add user to project (ADMIN or project MODERATOR)
- `DELETE /api/projects/{projectId}/users/{userId}` - Remove user from project (ADMIN or project MODERATOR)

### Bank Statements (Authenticated)
- `GET /api/bank-statements` - List user's statements
- `GET /api/bank-statements?startDate=...&endDate=...` - Filter by date range
- `POST /api/bank-statements` - Create new statement
- `DELETE /api/bank-statements/{id}` - Delete statement

### Donations (Authenticated)
- `GET /api/donations` - List user's donations
- `POST /api/donations/calculate?startDate=...&endDate=...&percentage=1` - Calculate donation
- `GET /api/donations/total` - Get total processed donations
- `POST /api/donations/{id}/process` - Mark donation as processed (ADMIN only)

**API Documentation:**
- Swagger UI: `http://localhost:8080/swagger-ui`
- OpenAPI Spec: `http://localhost:8080/openapi`

## Database Migrations

Flyway migrations are in `src/main/resources/db/migration/`:
- `V1.0.0__initial_schema.sql` - Creates tables: users, bank_statements, donations, addresses
- `V1.0.1__seed_data.sql` - Creates admin user (admin@incentive.com / admin123)
- `V1.0.2__add_projects_and_user_projects.sql` - Adds multi-project support:
  - Creates `projects` table
  - Creates `user_projects` table (many-to-many with roles)
  - Creates "Default Project" and associates all existing users to it

Migrations run automatically on startup (`quarkus.flyway.migrate-at-start=true`).

## Development Notes

### Panache Active Record Pattern
All entities extend `PanacheEntity` which provides:
- Auto-generated `id` field (Long)
- Built-in methods: `persist()`, `delete()`, `findById()`, `listAll()`
- Query methods: `find()`, `list()`, `stream()`, `count()`
- Custom static methods in entity classes for domain-specific queries

Example:
```java
// Finding users
User user = User.findById(1L);
List<User> all = User.listAll();
Optional<User> byEmail = User.findByEmail("test@example.com");

// Persisting
user.persist();
```

### Password Hashing
Use `PasswordUtil.hashPassword()` and `PasswordUtil.verifyPassword()` for BCrypt operations.

### Email Verification Flow
1. Registration generates 6-digit code stored in `user.verificationCode`
2. Code expires in 15 minutes (`user.verificationCodeExpiresAt`)
3. User must verify email before full access
4. Code can be resent via `/api/auth/resend-verification`

### Testing Locally
Default admin credentials for testing:
- Email: `admin@incentive.com`
- Password: `admin123`
- Role: ADMIN

## Important Implementation Details

1. **Transaction Management**: Use `@Transactional` on service methods that modify data. Scheduler methods already have it.

2. **Lazy Loading**: Entity relationships use `FetchType.LAZY`. Always access within transaction or use `@Transactional`.

3. **Date Handling**: Uses `LocalDate` for dates, `LocalDateTime` for timestamps. Time zones are not explicitly handled.

4. **Decimal Precision**: All monetary amounts use `BigDecimal` with `precision = 15, scale = 2`.

5. **Validation**: Jakarta Bean Validation annotations on DTOs and entities. Validation errors return 400 Bad Request.

6. **Logging**: Uses JBoss Logging. Debug level for `com.incentive` package, INFO for others.

## Frontend Integration

Backend is CORS-enabled for React/Vue/Angular frontends. Include JWT token in Authorization header:

```javascript
// Login returns user data + projects
const loginResponse = await fetch('http://localhost:8080/api/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ email, password })
});

const { token, user, projects } = await loginResponse.json();
// projects: [{ projectId, projectName, role, ... }]

// Use token in subsequent requests
fetch('http://localhost:8080/api/users/me', {
  headers: {
    'Authorization': `Bearer ${token}`
  }
})
```

**Multi-Project Frontend Considerations:**
- Display project selector in UI based on `projects` array from login
- Store current selected project in frontend state
- Filter data based on current project
- Show/hide UI elements based on user's role in current project
- MODERATOR and AGENT roles have project-specific permissions

## Deployment Considerations

- **Production Database**: Update datasource URL to production PostgreSQL (Neon, Railway, Render)
- **CORS**: Restrict `quarkus.http.cors.origins` to production frontend domain
- **JWT Keys**: Generate new RSA key pair for production (never commit private key)
- **Email**: Ensure Gmail App Password is set via environment variables
- **Environment Variables**: Use `QUARKUS_*` prefix for property overrides in deployment

## Common Tasks

**Add new REST endpoint:**
1. Create DTO in `dto/` package
2. Add method to resource class in `resource/`
3. Annotate with `@GET`, `@POST`, etc. and `@Path`
4. Add security annotations: `@RolesAllowed`, `@Authenticated`

**Add new entity:**
1. Create class extending `PanacheEntity` in `entity/`
2. Add JPA annotations and validation
3. Create Flyway migration in `db/migration/`
4. Add custom query methods as static methods

**Add scheduled task:**
1. Add method to `DonationScheduler` or create new scheduler class
2. Annotate with `@Scheduled(cron = "...")`
3. Add `@Transactional` if modifying data
4. Follow cron format: `second minute hour day month day-of-week`
