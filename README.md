# Voluntariado Hub — Spring Boot MVP

API local em **Java 17 + Spring Boot + JPA**, com H2 em memória para demonstrar rapidamente o funcionamento e opção de **PostgreSQL** via Docker Compose.

## Implementado
- Criar ação comunitária e turnos com capacidade, início e fim.
- Inscrever um identificador fictício de voluntário em um turno.
- Impedir inscrição duplicada e recusar vagas acima da capacidade.
- Consultar total ocupado e vagas restantes.
- Bloqueio pessimista de turno durante inscrição para proteger a capacidade em acessos concorrentes.

## Iniciar (H2 em memória)
```bash
mvn spring-boot:run
```
API local: http://127.0.0.1:8080

## Usar PostgreSQL local
```bash
docker compose up -d
SPRING_PROFILES_ACTIVE=postgres mvn spring-boot:run
```
No Windows PowerShell, defina `$env:SPRING_PROFILES_ACTIVE="postgres"` antes do comando Maven. Usuário e senha em `compose.yml` são **somente para desenvolvimento local** e devem ser substituídos em qualquer ambiente compartilhado.

## Rotas
- `POST /api/activities` — `{"title":"Mutirão de limpeza"}`
- `POST /api/shifts` — `{"activityId":1,"capacity":2,"startsAt":"2026-11-08T09:00:00","endsAt":"2026-11-08T11:00:00"}`
- `POST /api/shifts/1/enrollments` — `{"volunteerCode":"voluntario-ficticio-1"}`
- `GET /api/shifts/1` — vagas ocupadas/restantes

## Testes
```bash
mvn test
```
O teste integra a API e verifica que uma terceira inscrição é rejeitada quando só há duas vagas.

## Limites
API demonstrativa sem contas reais, autenticação e autorização. O perfil H2 perde dados ao reiniciar; PostgreSQL permite persistência local. A criação/atualização automática de tabelas é para desenvolvimento; produção requer migrações versionadas, controle de acesso e política de dados pessoais.
