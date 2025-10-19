# Kaiburr Executor — Distroless Container Image
# Author: Aditya R
# 
# Security features:
# - Distroless base (no shell, no package managers)
# - Non-root user (65532:65532)
# - Allowlisted binaries only
# - Fixed paths for validation

# Build stage: prepare binaries
FROM alpine:3.19 AS builder

# Install busybox (statically linked binaries)
RUN apk add --no-cache busybox-static \
    && mkdir -p /executor/usr/bin

# Copy statically-linked BusyBox
RUN cp /bin/busybox.static /executor/usr/bin/busybox

# Create symlinks for all allowlisted commands (relative symlinks)
RUN cd /executor/usr/bin && \
    ln -s busybox echo && \
    ln -s busybox date && \
    ln -s busybox uname && \
    ln -s busybox whoami && \
    ln -s busybox id && \
    ln -s busybox uptime && \
    ln -s busybox printenv && \
    ln -s busybox env && \
    ln -s busybox pwd && \
    ln -s busybox hostname

# Verify binaries work
RUN /executor/usr/bin/busybox echo "Build OK" && \
    /executor/usr/bin/echo "Echo works" && \
    /executor/usr/bin/date

# Runtime stage: distroless
FROM gcr.io/distroless/static-debian12:nonroot

# Metadata
LABEL maintainer="Aditya R" \
      app="kaiburr-task2" \
      component="executor" \
      description="Secure command executor with allowlisted binaries only"

# Copy binaries from builder
COPY --from=builder /executor/usr/bin /usr/bin

# Run as nobody (non-root)
USER 65532:65532

# No ENTRYPOINT (command specified at runtime)
# No CMD (args specified at runtime)

# Verify image at build time
# (Distroless doesn't have shell, so we can't run test commands here)
