# ADR-0004: Policy-as-Data for Command Validation

**Status:** Accepted  
**Date:** 2025-10-19  
**Author:** Aditya R

## Context

Command validation rules must be:
- **Externalizable** — Change without recompiling code
- **Auditable** — Track policy changes in version control
- **Testable** — Unit test policy loading separately from validation logic
- **Reloadable** — Update policies without service restart

We must choose a format and loading mechanism.

## Decision

Store validation rules in **YAML** (`command-policy.yaml`) and implement **hot-reload** via file watcher.

## Format

```yaml
# command-policy.yaml
denylist:
  commands:
    - rm
    - sudo
    - reboot
    - shutdown
    - halt
    - kill
    - pkill
    - nc
    - ncat
    - curl
    - wget
    - scp
    - ssh
    - iptables
    - systemctl
    - service
    - mkfs
    - dd
    - chown
    - chmod
  metacharacters:
    - ";"
    - "|"
    - "&"
    - ">"
    - "<"
    - "`"
    - "$"
    - "("
    - ")"
    - "{"
    - "}"
    - "["
    - "]"
    - "*"
    - "?"
    - "!"
    - "~"
    - "\""
    - "'"
    - "\\"
    - "\n"
    - "\r"
  sequences:
    - "&&"
    - "||"
    - "../"

allowlist:
  binaries:
    - echo
    - date
    - uname
    - whoami
    - id
    - uptime
    - printenv

limits:
  maxArgs: 8
  maxTotalLength: 200
  argumentPattern: "^[A-Za-z0-9._:/=-]{1,64}$"

timeouts:
  apiClientSeconds: 30
  jobActiveDeadlineSeconds: 15

output:
  maxStdoutBytes: 131072   # 128 KiB
  maxStderrBytes: 65536    # 64 KiB
```

## Rationale

### Why YAML Over Alternatives

| Format | Pros | Cons | Decision |
|--------|------|------|----------|
| **YAML** | Human-readable, comments, Spring Boot native | Indentation-sensitive | ✅ Chosen |
| **JSON** | Strict parsing, widely supported | No comments, verbose | ❌ Less readable |
| **TOML** | Simple syntax, typed | Less common, requires library | ❌ Uncommon |
| **Java Config** | Type-safe, IDE support | Requires recompile | ❌ Not externalizable |
| **Database** | Centralized, versioned | Adds dependency, latency | ❌ Over-engineered |

### Hot-Reload Implementation

```java
@Component
public class CommandValidator {
    
    private volatile CommandPolicy policy;
    
    @PostConstruct
    public void init() {
        loadPolicy();
        watchPolicyFile();
    }
    
    private void watchPolicyFile() {
        WatchService watcher = FileSystems.getDefault().newWatchService();
        Path path = Paths.get("src/main/resources/command-policy.yaml");
        path.getParent().register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
        
        executor.submit(() -> {
            while (true) {
                WatchKey key = watcher.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.context().toString().equals("command-policy.yaml")) {
                        log.info("Policy file modified, reloading...");
                        loadPolicy();
                    }
                }
                key.reset();
            }
        });
    }
    
    private void loadPolicy() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        this.policy = mapper.readValue(
            new ClassPathResource("command-policy.yaml").getInputStream(),
            CommandPolicy.class
        );
        log.info("Loaded command policy: {} allowlisted binaries, {} denylisted commands",
                 policy.getAllowlist().getBinaries().size(),
                 policy.getDenylist().getCommands().size());
    }
}
```

### Validation Logic

1. **Denylist Check** — Reject if command in `denylist.commands`
2. **Metacharacter Check** — Reject if any arg contains `denylist.metacharacters` or `sequences`
3. **Allowlist Check** — Reject if command not in `allowlist.binaries`
4. **Length Check** — Reject if total length > `limits.maxTotalLength`
5. **Arg Count Check** — Reject if args > `limits.maxArgs`
6. **Arg Pattern Check** — Reject if any arg doesn't match `limits.argumentPattern`

### Kubernetes ConfigMap Integration

The same `command-policy.yaml` is mounted into executor pods via ConfigMap:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: kaiburr-policy
  namespace: kaiburr
data:
  command-policy.yaml: |
    # (same content as above)
```

This ensures **policy consistency** between API validation and executor enforcement.

## Alternatives Considered

### Option 1: Hardcoded Policy in Code

**Pros:**
- Type-safe at compile time
- No parsing errors

**Cons:**
- Requires recompile and redeploy for changes
- Harder to audit policy changes
- Cannot A/B test policies

**Rejected** due to inflexibility.

### Option 2: Database-Backed Policy

**Pros:**
- Centralized, versioned
- Multi-service support

**Cons:**
- Adds latency to validation path
- Requires DB migrations for policy changes
- Over-engineered for single-service use case

**Rejected** as over-engineered.

### Option 3: Remote Policy Service (OPA)

**Pros:**
- Industry-standard (Open Policy Agent)
- Rego policy language
- Advanced features (RBAC, context-aware)

**Cons:**
- Extra service dependency
- Added complexity and latency
- Overkill for simple command validation

**Deferred** — consider for multi-tenant deployment.

## Consequences

### Positive

- Policy changes without code deploy
- Version control tracks policy history
- Hot-reload enables rapid iteration
- Same policy enforced in API and executor
- Easy to test: just load YAML and validate

### Negative

- File I/O on startup and reload
- Potential race condition during reload (mitigated with `volatile`)
- YAML syntax errors can break reload (need error handling)

### Migration Path

Future enhancements:
- **Policy Versioning** — Include `version` field, API returns policy version used
- **Per-Task Policy** — Override global policy per task
- **Policy Audit Log** — Log policy changes with timestamp
- **Remote Policy Source** — Fetch from Git/S3 for multi-instance consistency

## Implementation Checklist

- [x] Define `CommandPolicy` POJO matching YAML structure
- [x] Load policy on application startup
- [x] Implement file watcher for hot-reload
- [x] Handle YAML parse errors gracefully
- [x] Log policy load/reload events
- [x] Create ConfigMap with same policy for executor
- [x] Unit test policy loading and validation
- [x] Document policy format in `command-policy.yaml` comments

## References

- [Spring Boot Configuration Properties](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [Java WatchService API](https://docs.oracle.com/javase/tutorial/essential/io/notification.html)
- [OWASP Command Injection Prevention](https://cheatsheetseries.owasp.org/cheatsheets/OS_Command_Injection_Defense_Cheat_Sheet.html)
