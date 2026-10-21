# Task Tracker — навчальний проєкт (режим ментора)

## Роль Claude
- Claude — **вчитель/ментор**, а не виконавець. **НЕ створювати і НЕ редагувати файли з кодом** (Java, SQL, yaml, pom, Dockerfile, compose), поки користувач прямо не попросить. Код показувати в чаті, користувач переписує його вручну.
- Виняток: цей `CLAUDE.md` можна оновлювати, зокрема чекліст прогресу.
- Вести маленькими кроками: **один крок = один клас/файл**. Перед кодом коротко пояснити, що робимо і навіщо.
- На «чому?» пояснювати, як це працює під капотом (Spring, Hibernate, JPA).
- Час від часу ставити 1–2 короткі питання на розуміння, як на співбесіді.
- Залежності підключати поступово, тільки коли до них доходимо.
- Коли користувач показує свій код: перевірити його, вказати на помилки і пояснити їх, але не переписувати все за нього.
- Пріоритет: розуміння і чистий код важливіші за кількість фіч.
- Мова спілкування: **українська**.

## Стек
Java 21, Spring Boot **4.1.1**, Maven (`./mvnw`), PostgreSQL (у Docker з першого кроку), Flyway (`ddl-auto: validate`), Spring Data JPA, Bean Validation, MapStruct + Lombok, Spring Security + JWT (`jjwt` + власний `OncePerRequestFilter`), springdoc-openapi 3.x, JUnit 5, Mockito, Testcontainers, Docker Compose.

Нюанси Boot 4: Hibernate 7, Spring Security 7, Jackson 3 (пакет `tools.jackson`), `@MockitoBean` замість `@MockBean`, модульні стартери (`spring-boot-starter-webmvc`, `*-test`).
MapStruct: процесор і `lombok-mapstruct-binding` додавати в **обидва** execution-и `annotationProcessorPaths` у pom.

## Архітектура
Базовий пакет `com.example.tasktracker`, організація package-by-feature:
```
common/   exception/ (свої винятки, GlobalExceptionHandler), config/
security/ JwtService, JwtAuthenticationFilter, SecurityConfig, AppUserDetails(Service)
auth/     AuthController, AuthService, dto/
user/     User, Role, UserRepository, UserService, UserController, dto/, UserMapper
project/  Project, ProjectRepository, ProjectService, ProjectController, dto/, ProjectMapper
task/     Task, TaskStatus, TaskPriority, TaskRepository, TaskSpecifications, TaskService, TaskController, dto/, TaskMapper
comment/  Comment, CommentRepository, CommentService, CommentController, dto/, CommentMapper
```

### Сутності
- **User**: id, email (unique), passwordHash, fullName, role (USER/ADMIN), createdAt
- **Project**: id, name, description, createdAt; `@ManyToOne owner`; `@OneToMany(mappedBy) tasks`
- **Task**: id, title, description, status (TODO/IN_PROGRESS/DONE), priority (LOW/MEDIUM/HIGH), deadline (LocalDate), createdAt, updatedAt; `@ManyToOne project / assignee (nullable) / author`; `@OneToMany(mappedBy) comments`
- **Comment**: id, text, createdAt; `@ManyToOne task / author`

### Правила
- Усі `@ManyToOne` мають `fetch = LAZY`. Enum зберігаємо через `@Enumerated(STRING)`. `spring.jpa.open-in-view: false`.
- Схему змінюємо **тільки** міграціями Flyway (`db/migration/V{n}__*.sql`).
- Entity не виходять за межі сервісу. DTO робимо як `record`, мапінг через MapStruct.
- На entity не ставимо `@Data`: `equals/hashCode` пишемо по id.
- Фільтри робимо через `Specification`, пагінацію через `Pageable`, відповідь віддаємо в `PagedModel`.
- Помилки повертає `@RestControllerAdvice` у форматі `ProblemDetail` (RFC 9457).
- Доступ: USER бачить задачі, де він автор або виконавець, а також задачі у своїх проєктах. ADMIN бачить усе.

### Ендпоінти
```
POST   /api/auth/register | /api/auth/login
GET    /api/users/me ;  GET /api/users (ADMIN)
GET    /api/projects?page&size&sort ; POST ; GET/PUT/DELETE /api/projects/{id}
POST   /api/projects/{projectId}/tasks
GET    /api/tasks?status&priority&assigneeId&projectId&deadlineBefore&q&page&size&sort
GET/PUT/DELETE /api/tasks/{id} ;  PATCH /api/tasks/{id}/status
GET    /api/tasks/{taskId}/comments?page ; POST /api/tasks/{taskId}/comments
DELETE /api/comments/{id}
```

## Роадмап і прогрес
Позначати `[x]`, коли крок готовий і перевірений.

**Етап 0. Середовище**
- [x] 0.1 `compose.yaml`: тільки Postgres (healthcheck, volume), `docker compose up -d`
- [x] 0.2 `application.yaml`: datasource, `ddl-auto: validate`, `open-in-view: false`, SQL-логи; перший запуск

**Етап 1. User**
- [x] 1.1 `V1__create_users.sql`
- [x] 1.2 `Role`
- [x] 1.3 `User` entity
- [x] 1.4 `UserRepository`

**Етап 2. Project (повний CRUD-зріз)**
- [x] 2.1 `V2` міграція
- [x] 2.2 `Project`
- [x] 2.3 `ProjectRepository`
- [x] 2.4 DTO + валідація
- [x] 2.5 MapStruct + `ProjectMapper`
- [x] 2.6 `ProjectService`
- [x] 2.7 `ProjectController` (Pageable; поки власник береться з `X-User-Id`)

**Етап 3. Обробка помилок**
- [ ] 3.1 Свої винятки
- [ ] 3.2 `GlobalExceptionHandler`

**Етап 4. Task**
- [ ] 4.1 `V3` міграція + індекси
- [ ] 4.2 `TaskStatus`, `TaskPriority`
- [ ] 4.3 `Task`
- [ ] 4.4 `TaskRepository`
- [ ] 4.5 `TaskSpecifications`
- [ ] 4.6 DTO + `TaskFilter`
- [ ] 4.7 `TaskMapper`
- [ ] 4.8 `TaskService`
- [ ] 4.9 `TaskController`

**Етап 5. Comment**
- [ ] 5.1 міграція → entity → repo → DTO → mapper → service → controller

**Етап 6. Лабораторія N+1**
- [ ] 6.1 Відтворити N+1 на `GET /api/tasks` і побачити його в логах
- [ ] 6.2 Фікс через `@EntityGraph` / `JOIN FETCH`
- [ ] 6.3 Пастка «fetch колекції + пагінація» (`HHH90003004`), `@BatchSize`

**Етап 7. Security + JWT**
- [ ] 7.1 Залежності
- [ ] 7.2 `AppUserDetails(Service)`
- [ ] 7.3 `PasswordEncoder`
- [ ] 7.4 `JwtService`
- [ ] 7.5 `JwtAuthenticationFilter`
- [ ] 7.6 `SecurityConfig`
- [ ] 7.7 `AuthService` + `AuthController`
- [ ] 7.8 `X-User-Id` замінити на `@AuthenticationPrincipal`; правила доступу; `@PreAuthorize`
- [ ] 7.9 `V5` seed admin

**Етап 8. Swagger**
- [ ] 8.1 springdoc + `OpenApiConfig` (bearer)

**Етап 9. Тести**
- [ ] 9.1 `TaskServiceTest` (Mockito)
- [ ] 9.2 `@WebMvcTest` `TaskController`
- [ ] 9.3 Testcontainers: `TaskRepositoryIT`
- [ ] 9.4 Повний сценарій IT (auth → CRUD → чужий юзер отримує 403/404)

**Етап 10. Застосунок у Docker**
- [ ] 10.1 Multi-stage `Dockerfile`
- [ ] 10.2 Сервіс `app` у `compose.yaml`, `docker compose up`

## Команди
- БД: `docker compose up -d` / `docker compose down` (з `-v` видаляє дані)
- Запуск: `./mvnw spring-boot:run`
- Тести: `./mvnw test`, з інтеграційними: `./mvnw verify` (потрібен Docker)