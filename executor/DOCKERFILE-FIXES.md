# Dockerfile Fixes Applied

**Date:** October 19, 2025  
**Author:** Aditya R

---

## Issue: Docker Build Failed (Exit Code 1)

### Root Cause Analysis

#### Problem 1: Incorrect Binary Path
```
cp: cannot stat '/usr/bin/printenv': No such file or directory
```

**Resolution:** Changed from `/usr/bin/printenv` to `/bin/printenv` based on Alpine Linux binary locations.

#### Problem 2: Dynamic Linking Dependencies
After fixing paths, the binaries failed to run in the distroless container:
```
exec /usr/bin/echo: no such file or directory
Error loading shared library libcrypto.so.3: No such file or directory
Error loading shared library libacl.so.1: No such file or directory
```

**Resolution:** Switched from `coreutils` (dynamically linked, many dependencies) to `busybox-static` (statically linked, zero dependencies).

---

## Final Solution

### Key Changes

1. **Use BusyBox Static Binary**
   - Statically linked (no shared library dependencies)
   - Works in minimal distroless container
   - Single binary (~1MB) provides all 10 commands via symlinks

2. **Relative Symlinks**
   - Created symlinks using relative paths (`ln -s busybox echo`)
   - Ensures symlinks work when copied to distroless image

3. **Minimal Image Size**
   - Final image: **7.93 MB** (vs. 19.7 MB with coreutils)
   - Security: Runs as non-root user (uid=65532)
   - Zero attack surface: No shell, no package manager, only allowlisted binaries

---

## Verification

### Build Success
```powershell
docker build -t kaiburr-executor:dev .
# ✅ [+] Building 1.9s (12/12) FINISHED
```

### Runtime Tests
```powershell
# Test echo
docker run --rm kaiburr-executor:dev /usr/bin/echo "Hello Kaiburr"
# Output: Hello Kaiburr ✅

# Test date
docker run --rm kaiburr-executor:dev /usr/bin/date
# Output: Sun Oct 19 13:14:54 UTC 2025 ✅

# Test uname with args
docker run --rm kaiburr-executor:dev /usr/bin/uname -a
# Output: Linux ... ✅

# Verify non-root user
docker run --rm kaiburr-executor:dev /usr/bin/id
# Output: uid=65532(nonroot) gid=65532(nonroot) ✅
```

---

## Technical Details

### BusyBox Applets Provided
All 10 allowlisted commands are provided via a single statically-linked binary:
- `echo` - Print arguments
- `date` - Show/set date and time
- `uname` - Print system information
- `whoami` - Print current username
- `id` - Print user/group IDs
- `uptime` - Show system uptime
- `printenv` - Print environment variables
- `env` - Run command with modified environment
- `pwd` - Print working directory
- `hostname` - Get/set hostname

### Security Properties
- ✅ Statically linked (no dependencies)
- ✅ Distroless base (no shell, no apt/apk)
- ✅ Non-root user (65532:65532)
- ✅ Read-only filesystem compatible
- ✅ No network tools
- ✅ No file manipulation tools beyond allowlist

### Image Comparison

| Approach | Base | Size | Dependencies | Status |
|----------|------|------|--------------|--------|
| coreutils + libs | Alpine + distroless | 19.7 MB | libcrypto, libacl, libattr, libutmps | ❌ Too many deps |
| busybox-static | Alpine + distroless | 7.93 MB | None (static) | ✅ Working |

---

## Lessons Learned

1. **Alpine vs Distroless Paths:** Alpine binary locations differ; always verify with `which` command
2. **Static > Dynamic:** For minimal containers, prefer static binaries to avoid dependency hell
3. **BusyBox Efficiency:** One static binary provides multiple commands via symlinks
4. **Symlink Portability:** Use relative paths for symlinks that will be copied to different filesystems

---

## Next Steps

The executor image is now ready for:
1. Loading into kind cluster: `kind load docker-image kaiburr-executor:dev --name kaiburr`
2. Kubernetes Job execution with security policies
3. Production deployment with resource limits and network policies

---

**Status:** ✅ All issues resolved - Executor image builds and runs successfully
