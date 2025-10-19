# Fix CI Build Failure

## Issue
GitHub Actions workflow "Build and Test" failing at "Run unit tests" step.

## Root Causes
1. ✅ KubernetesCommandRunner.java line 150: Integer to Long cast needed
2. ✅ KubernetesCommandRunner.java line 236: Missing parameters in listNamespacedPod
3. ⚠️ Possible test dependencies or test class issues

## Fixes Applied

### 1. KubernetesCommandRunner.java Type Casting
```java
// Line 150 - Cast activeDeadlineSeconds to Long
.activeDeadlineSeconds((long) activeDeadlineSeconds)
```

### 2. KubernetesCommandRunner.java API Call
```java
// Line 236 - Complete listNamespacedPod signature
V1PodList pods = coreApi.listNamespacedPod(
    namespace, null, null, null, null, labelSelector,
    null, null, null, null, null, null
);
```

### 3. Verify Test Dependencies in pom.xml
Ensure all test dependencies are present:
- junit-jupiter
- spring-boot-starter-test
- testcontainers-mongodb

## Commands to Rebuild

```bash
# Clean and rebuild
mvn clean install -DskipTests

# Run tests locally
mvn test

# If tests pass locally, commit and push
git add .
git commit -m "fix: resolve compilation errors in KubernetesCommandRunner"
git push origin main
```

## Verification Checklist

Before pushing:
- [ ] `mvn clean compile` succeeds
- [ ] `mvn test` passes locally
- [ ] No warnings about missing dependencies
- [ ] All Java files have correct imports
- [ ] application.yml has correct profiles

## If Still Failing

Check GitHub Actions logs for:
1. Specific compilation error messages
2. Missing dependencies
3. Test failures (not compilation)
4. MongoDB connection issues in tests

View logs at: https://github.com/YOUR_USERNAME/Kaiburr_Task2/actions
