# Microservices Migration Plan: Core Gateway + User Service

## Current Architecture
- **beaver-core**: Monolithic Spring Boot application
  - Handles authentication & JWT management
  - Stores user data (PostgreSQL)
  - Manages user CRUD operations
  - Acts as API gateway

## Target Architecture

### 1. Core API Gateway (this project)
**Responsibilities:**
- Entry point for all client requests
- JWT token generation & validation
- Request routing to downstream services
- Rate limiting & security headers
- Service-to-service authentication

**What it WILL keep:**
- JWT token generation/validation logic
- Authentication endpoints (`/auth/signin`, `/auth/signup`)
- Rate limiting filters
- Security configuration
- Redis caching for tokens/sessions

**What it WILL NOT have:**
- User data storage
- User CRUD operations
- Direct database access for users

### 2. User Service (new microservice)
**Responsibilities:**
- User data ownership & persistence
- User CRUD operations
- Credential validation
- User profile management
- User-specific business logic

## Authentication Flow Design

### 1. JWT Token Generation (Login/Signup)
```
Client → Gateway → User Service → Gateway → Client
```

**Steps:**
1. Client sends credentials to Gateway `/auth/signin`
2. Gateway calls User Service `/validate-credentials`
3. User Service validates credentials & returns user data
4. Gateway generates JWT tokens locally
5. Gateway returns tokens to client

**User Service Endpoints Needed:**
- `POST /api/v1/auth/validate-credentials`
- `POST /api/v1/users` (for signup)

### 2. Request Authorization (Every API call)
```
Client → Gateway → Downstream Service
```

**Steps:**
1. Client sends request with JWT to Gateway
2. Gateway validates JWT signature locally (no external calls)
3. Gateway extracts user info from JWT claims
4. Gateway forwards request with user context headers

## Service-to-Service Communication

### Security Strategy: Shared Secret + User Context Headers

**Gateway → User Service:**
```http
POST /api/v1/users/123
Authorization: Bearer service_shared_secret
X-User-Id: uuid-from-jwt
X-User-Email: user@example.com
X-User-Roles: USER,ADMIN
Content-Type: application/json
```

**Benefits:**
- Simple implementation
- No per-request authentication overhead
- Services can trust Gateway-provided user context
- Shared secret validates request is from Gateway

### Alternative: mTLS (Future Consideration)
- More secure but complex
- Certificate management overhead
- Consider for production/sensitive data

## Migration Strategy

### Phase 1: Extract User Service
1. Create new `user-service` Spring Boot project
2. Copy user-related code (entities, repositories, services)
3. Implement user service API endpoints
4. Add service-to-service authentication

### Phase 2: Update Core Gateway
1. Remove user database tables/entities
2. Replace direct user DB calls with HTTP client calls to user service
3. Keep JWT logic in Gateway
4. Update authentication flow to call user service

### Phase 3: Configuration & Deployment
1. Configure Spring Cloud Gateway routing
2. Update Docker Compose for multi-service deployment
3. Service discovery setup (if needed)
4. Update CI/CD pipelines

## User Service API Design

### Authentication Endpoints
```
POST /api/v1/auth/validate-credentials
POST /api/v1/auth/signup
```

### User Management Endpoints
```
GET    /api/v1/users/{userId}
PATCH  /api/v1/users/{userId}
PUT    /api/v1/users/{userId}/credentials
DELETE /api/v1/users/{userId}
```

### Headers for Service-to-Service Auth
```
Authorization: Bearer {service_shared_secret}
X-User-Id: {extracted_from_jwt}
X-User-Email: {extracted_from_jwt}
X-User-Roles: {extracted_from_jwt}
```

## Technology Stack

### Core Gateway
- Spring Boot 3.x
- Spring Cloud Gateway
- Spring Security (JWT)
- Redis (caching)
- No user database

### User Service
- Spring Boot 3.x
- Spring Data JPA
- PostgreSQL (user data)
- Redis (caching)

## Questions to Resolve

### 1. Token Validation Strategy
**Trust JWT claims completely
- Pro: No external calls, fast
- Con: Can't revoke tokens immediately

### 2. Service-to-Service Security
**Current Plan:** Shared secret in headers
- Simple to implement
- Sufficient for internal network
- Can upgrade to mTLS later

### 3. Configuration Management
**Spring Cloud Gateway Routing:**
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: http://user-service:8081
          predicates:
            - Path=/api/v1/users/**
          filters:
            - AddRequestHeader=Authorization,Bearer ${SERVICE_SHARED_SECRET}
            - AddRequestHeader=X-User-Id,{jwt.sub}
            - AddRequestHeader=X-User-Email,{jwt.email}
```

**Note:** 
- `/api/v1/auth/**` endpoints are handled locally by the Gateway (no routing needed)
- Only `/api/v1/users/**` requests are routed to the User Service
- Gateway adds service authentication headers automatically

## Implementation Checklist

### User Service Creation
- [ ] Create new Spring Boot project
- [ ] Copy user entity & repository
- [ ] Implement authentication endpoints
- [ ] Add service-to-service auth validation
- [ ] Create Docker configuration

### Gateway Updates  
- [ ] Add HTTP client for user service
- [ ] Update authentication controller
- [ ] Remove user database dependencies
- [ ] Add Spring Cloud Gateway
- [ ] Configure service routing

### Infrastructure
- [ ] Update Docker Compose
- [ ] Configure shared secrets
- [ ] Update environment variables
- [ ] Test service communication

This plan provides a clear roadmap for extracting the user service while maintaining security and performance.