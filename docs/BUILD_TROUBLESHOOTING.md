# Build troubleshooting

## Observed errors
- `./gradlew: Permission denied`
- `IllegalArgumentException: 25.0.1` while running Gradle with JDK 25
- Plugin resolution failure for `com.android.application` when repositories are unreachable

## Fixes
1. Ensure wrapper is executable: `chmod +x gradlew`.
2. Use JDK 17 for this Android project. A `.java-version` file is added with `17` for local tool managers.
3. If plugin resolution fails in restricted networks, configure proxy/mirror access for Google Maven and Gradle Plugin Portal.
