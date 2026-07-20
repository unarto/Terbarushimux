# Custom Instructions for Gemini Models

## Strict System Constraints

1. **Gradle Build Security (No CMake/NDK Modification)**:
   - You must NEVER modify CMake version configurations, NDK paths, or SDK tool parameters in `build.gradle.kts` or `build.gradle` files.
   - Modifying these versions disrupts native platform compilation paths, leading to CMake or compiler resolution failures. Keep configurations locked.

2. **No Placeholders or Fictional Assets**:
   - Do NOT write mock architectures, simulated responses, placeholder methods, or mock UI state values.
   - Do NOT ship fake or corrupted binary placeholders (`.so`, `.bin`, `.keystore`) under any circumstance.
   - All code written must be production-ready, fully typed, and structured cleanly to ensure debugging tracks genuine errors.

3. **Behavioral Compliance**:
   - Maintain the architectural boundaries of the codebase.
   - Ensure all updates are documented in `/PROGRESS_MAP.md`.
