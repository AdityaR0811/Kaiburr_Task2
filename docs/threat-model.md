# Threat Model - Kaiburr Task 1

**Author**: Aditya R.  
**Date**: 2025-10-18  
**Methodology**: STRIDE

## System Overview

Kaiburr Task 1 is a REST API service that allows users to create tasks with system commands and execute them. The service validates commands against a strict security policy before execution.

### Trust Boundaries

1. **External Client** → **REST API** (untrusted input)
2. **REST API** → **MongoDB** (trusted internal)
3. **Application** → **Host OS** (privileged operation)

## STRIDE Analysis

### S - Spoofing Identity

| Threat | Impact | Likelihood | Mitigation | Status |
|--------|--------|------------|------------|--------|
| **T1.1**: Attacker impersonates legitimate user | High | Medium | ✅ **Planned**: OAuth2/JWT authentication in Task 4 | Future |
| **T1.2**: Correlation ID spoofing to hide audit trail | Medium | Low | ✅ **Implemented**: Server generates correlation ID if not provided | Done |
| **T1.3**: MongoDB credentials leaked | High | Low | ✅ **Implemented**: Environment variables, no hard-coded secrets | Done |

**Code Reference**:
```java
// CorrelationIdFilter.java
String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);
if (correlationId == null || correlationId.trim().isEmpty()) {
    correlationId = CorrelationId.generate();  // Server-controlled
}
```

### T - Tampering with Data

| Threat | Impact | Likelihood | Mitigation | Status |
|--------|--------|------------|------------|--------|
| **T2.1**: Modify command after validation | Critical | Low | ✅ **Implemented**: Command validated immediately before execution | Done |
| **T2.2**: MongoDB injection via task name/owner | High | Medium | ✅ **Implemented**: Spring Data MongoDB parameterized queries | Done |
| **T2.3**: Concurrent update race condition | Medium | Medium | ✅ **Implemented**: Optimistic locking with `@Version` | Done |
| **T2.4**: Audit log tampering | High | Low | ⚠️ **Partial**: Append-only file, should add integrity checking (HMAC) | Partial |

**Code Reference**:
```java
// Task.java
@Version
private Long version;  // Prevents lost updates

// TaskRepository.java - Parameterized query prevents injection
@Query("{ 'name': { $regex: ?0, $options: 'i' } }")
List<Task> findByNameContainingIgnoreCase(String name);
```

### R - Repudiation

| Threat | Impact | Likelihood | Mitigation | Status |
|--------|--------|------------|------------|--------|
| **T3.1**: User denies executing malicious command | Medium | High | ✅ **Implemented**: Audit log with correlation ID, owner, command hash | Done |
| **T3.2**: Missing audit entries | Low | Low | ⚠️ **Partial**: Append-only log, no rotation yet | Partial |
| **T3.3**: Audit log not centralized | Medium | N/A | ✅ **Planned**: ELK stack or Splunk integration in production | Future |

**Code Reference**:
```java
// TaskService.java - Audit log entry
Map<String, Object> auditEntry = new HashMap<>();
auditEntry.put("timestamp", Instant.now().toString());
auditEntry.put("correlationId", execution.getCorrelationId());
auditEntry.put("owner", task.getOwner());
auditEntry.put("commandHash", hashCommand(task.getCommand()));
```

### I - Information Disclosure

| Threat | Impact | Likelihood | Mitigation | Status |
|--------|--------|------------|------------|--------|
| **T4.1**: Command output contains sensitive data | High | High | ✅ **Implemented**: Output truncated at 128 KiB | Done |
| **T4.2**: Error messages reveal internal paths | Medium | Medium | ✅ **Implemented**: Generic error messages, no stack traces in API | Done |
| **T4.3**: Swagger UI exposed in production | Low | High | ⚠️ **Partial**: Should disable in prod or add auth | Partial |
| **T4.4**: Actuator endpoints reveal metrics | Medium | Medium | ⚠️ **Partial**: Endpoints exposed, should add Spring Security | Partial |
| **T4.5**: MongoDB connection string in logs | High | Low | ✅ **Implemented**: Passwords masked in logs | Done |

**Code Reference**:
```java
// application.yml - No stack traces
server:
  error:
    include-stacktrace: never
    include-exception: false

// SafeProcessIO.java - Output truncation
if (totalBytes >= maxBytes) {
    output.append("\n[OUTPUT TRUNCATED - LIMIT REACHED]");
}
```

### D - Denial of Service

| Threat | Impact | Likelihood | Mitigation | Status |
|--------|--------|------------|------------|--------|
| **T5.1**: Fork bomb via command execution | Critical | High | ✅ **Implemented**: 5-second timeout, process killed | Done |
| **T5.2**: Memory exhaustion from large outputs | High | High | ✅ **Implemented**: Output truncated at 128 KiB stdout, 64 KiB stderr | Done |
| **T5.3**: MongoDB overload from many tasks | Medium | Medium | ⚠️ **Planned**: Rate limiting per user in Task 4 | Future |
| **T5.4**: Long-running queries | Low | Low | ✅ **Implemented**: Indexed name field for fast searches | Done |
| **T5.5**: Disk fill from audit logs | Medium | Low | ⚠️ **Partial**: Log rotation not implemented | Partial |

**Code Reference**:
```java
// SafeProcessIO.java - Timeout protection
boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
if (!completed) {
    process.destroyForcibly();  // Kill process
    throw new TimeoutException("Process execution timed out after " + timeoutSeconds + " seconds");
}

// command-policy.yaml - Limits
limits:
  maxCommandLength: 200
  maxArgs: 8
  timeoutSeconds: 5
  maxStdoutBytes: 131072  # 128 KiB
  maxStderrBytes: 65536   # 64 KiB
```

### E - Elevation of Privilege

| Threat | Impact | Likelihood | Mitigation | Status |
|--------|--------|------------|------------|--------|
| **T6.1**: Command injection via shell metacharacters | Critical | High | ✅ **Implemented**: No shell invocation, direct binary execution | Done |
| **T6.2**: Path traversal to execute arbitrary binaries | Critical | High | ✅ **Implemented**: Allowlist of binaries only | Done |
| **T6.3**: Sudo/root command execution | Critical | High | ✅ **Implemented**: Denylist blocks sudo, root commands | Done |
| **T6.4**: Escape from command validator | Critical | Low | ✅ **Implemented**: Multi-layer validation (length, chars, tokens, sequences) | Done |
| **T6.5**: JVM exploitation | High | Very Low | ✅ **Implemented**: Latest JDK 17, dependencies updated | Done |

**Code Reference**:
```java
// LocalCommandRunner.java - NO SHELL
String[] parts = command.trim().split("\\s+");
ProcessBuilder pb = new ProcessBuilder(parts);  // Direct execution

// CommandValidator.java - Multiple checks
- Binary must be in allowlist
- No denied tokens (rm, sudo, etc.)
- No metacharacters (;, |, &, $, etc.)
- No sequences (&&, ||, ../)
- Args match [A-Za-z0-9._:/=-] only
```

## Attack Scenarios

### Scenario 1: Command Injection Attempt

**Attack**:
```bash
curl -X PUT http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"id":"evil","name":"Pwn","owner":"hacker","command":"echo hello; rm -rf /"}'
```

**Defense Layers**:
1. ✅ Semicolon detected by metacharacter check → **REJECTED**
2. ✅ "rm" detected by denylist → **REJECTED**
3. ✅ Even if bypassed, ProcessBuilder would execute literal ";" as arg (fails)

**Result**: ❌ Attack fails with 400 Bad Request

### Scenario 2: Fork Bomb

**Attack**:
```bash
curl -X PUT http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"id":"bomb","name":"Fork Bomb","owner":"hacker","command":":(){ :|:& };:"}'
```

**Defense Layers**:
1. ✅ Colon, parentheses, braces detected by metacharacter check → **REJECTED**
2. ✅ Binary ":" not in allowlist → **REJECTED**
3. ✅ Even if executed, 5-second timeout kills process → **CONTAINED**

**Result**: ❌ Attack fails at validation

### Scenario 3: Path Traversal

**Attack**:
```bash
curl -X PUT http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"id":"traverse","name":"Traverse","owner":"hacker","command":"../../bin/cat /etc/passwd"}'
```

**Defense Layers**:
1. ✅ "../" sequence detected → **REJECTED**
2. ✅ Binary "../.." not in allowlist → **REJECTED**

**Result**: ❌ Attack fails with 400 Bad Request

### Scenario 4: Privilege Escalation

**Attack**:
```bash
curl -X PUT http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"id":"privesc","name":"Privesc","owner":"hacker","command":"sudo whoami"}'
```

**Defense Layers**:
1. ✅ "sudo" detected by denylist → **REJECTED**

**Result**: ❌ Attack fails with 400 Bad Request

## Residual Risks

| Risk | Severity | Mitigation Plan |
|------|----------|-----------------|
| **R1**: No authentication - anyone can execute commands | High | **Task 4**: Implement OAuth2/JWT |
| **R2**: No rate limiting - DoS via rapid requests | Medium | **Task 4**: Bucket4j rate limiter |
| **R3**: Swagger UI exposed without auth | Low | **Prod**: Disable or add Spring Security |
| **R4**: Actuator metrics public | Medium | **Prod**: Require authentication |
| **R5**: Audit logs on local disk (not tamper-proof) | Medium | **Future**: Ship to SIEM (Splunk/ELK) |
| **R6**: Command output may contain secrets | Medium | **Warning**: Document in README, consider redaction |

## Security Checklist

### Input Validation
- [x] Command length limit enforced
- [x] Argument count limit enforced
- [x] Character allowlist for args
- [x] Denylist for dangerous tokens
- [x] Metacharacter blocking
- [x] Newline and quote rejection
- [x] Binary allowlist enforcement

### Execution Safety
- [x] No shell invocation
- [x] Direct binary execution only
- [x] Process timeout enforced
- [x] Output size limits
- [x] Process killed on timeout
- [x] Async stream consumption

### Data Protection
- [x] MongoDB parameterized queries
- [x] Optimistic locking
- [x] No secrets in code
- [x] Environment-based config
- [ ] Encryption at rest (MongoDB)
- [ ] TLS/HTTPS (production)

### Auditing
- [x] Correlation ID tracking
- [x] Audit log for executions
- [x] Command hash in audit
- [ ] Log integrity (HMAC)
- [ ] Log rotation
- [ ] Centralized logging

### Access Control
- [ ] Authentication (OAuth2/JWT)
- [ ] Authorization (RBAC)
- [ ] Rate limiting
- [ ] IP whitelisting (if needed)

## Recommendations

### Immediate (Task 1)
1. ✅ All core security implemented
2. ⚠️ Add log rotation to prevent disk fill
3. ⚠️ Add HMAC to audit logs for integrity

### Short-term (Task 2-3)
1. Implement authentication/authorization
2. Add rate limiting
3. Disable Swagger UI in production
4. Secure Actuator endpoints

### Long-term (Production)
1. Deploy with TLS/HTTPS
2. Enable MongoDB encryption at rest
3. Ship audit logs to SIEM
4. Add WAF (Web Application Firewall)
5. Implement network segmentation
6. Regular security audits

## References

- OWASP Top 10 2021
- OWASP Command Injection Prevention Cheat Sheet
- CWE-77: Command Injection
- CWE-78: OS Command Injection
- STRIDE Threat Modeling (Microsoft)

---

**Document Version**: 1.0  
**Last Updated**: 2025-10-18  
**Reviewed By**: Aditya R.
