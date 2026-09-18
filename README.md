# SilenceProtector

SilenceProtector is a low-overhead Java obfuscation tool.

Its goal is not simply to make decompilation harder, but to make **full reconstruction of the original source structure and development intent significantly more difficult even after deobfuscation**.

---

## Goal

SilenceProtector does not attempt to make reverse engineering impossible.

Java bytecode must ultimately execute on the JVM, so sufficiently motivated analysts can still study program behavior.

Instead, SilenceProtector focuses on destroying source-level structure so that decompiled code is difficult to restore into a maintainable version of the original project.

---

## Core Principles

- Low runtime overhead
- Destruction of the original class and package structure
- Class, method, and field name obfuscation
- String and numeric constant protection
- Per-build randomized output
- Decompiled code that is difficult to maintain or reuse
- Minimal reliance on expensive runtime obfuscation

---

## Features

### Name Obfuscation

Renames classes, methods, and fields to remove semantic information.

Methods with annotations or actual override relationships can be preserved when required for compatibility.

### Package Randomization

The original package hierarchy can be discarded and classes can be redistributed into randomized package paths.

Package depth can be randomized from 1 to 3 levels, with an optional fixed root package.

Example:

```text
Original:
silence/simsool/mod/Example.class
silence/simsool/utils/Helper.class

Obfuscated:
silence/ab/X.class
silence/cd/ef/Y.class
```

### Fabric Entrypoint Support

When the main class is renamed or moved, SilenceProtector can automatically update the `entrypoints.client` value in `fabric.mod.json`.

### String Protection

String literals are converted into encrypted data and decrypted through `SLogic` at runtime.

Decrypted strings are cached per class to minimize repeated runtime overhead.

### Number Protection

Numeric constants such as `int`, `long`, `float`, and `double` can be transformed using multiple lightweight reversible rules.

Different constants can use different transformation rules and encoded values.

### Randomized SLogic

`SLogic`, which handles string and numeric decoding, is intended to support per-build randomized constants and transformation rules.

This allows the same input JAR to produce structurally different protected outputs across builds.

---

## Performance Philosophy

SilenceProtector avoids sacrificing substantial runtime performance purely for obfuscation strength.

It prioritizes build-time structural transformations such as:

- Name removal
- Package relocation
- Constant representation changes
- Class structure transformation
- One-time string decryption with caching

Heavy techniques are intentionally limited by default, including:

- Full-code virtualization
- Excessive reflection
- Large-scale control-flow flattening
- Repeated string decryption
- Excessive meaningless runtime operations

---

## Ultimate Goal

```text
Original Source
→ Compilation
→ Structural Analysis
→ Name and Metadata Removal
→ Package/Class Restructuring
→ String and Number Protection
→ Randomization
→ Obfuscated JAR
```

Even if the protected output can still be analyzed, the goal is to make the following information difficult to reconstruct accurately:

```text
Original class structure
Original package structure
Original names
Original code organization
Original design intent
```

The objective is not simply to break decompilers.

The objective is to produce **decompiled code that is difficult to maintain, extend, or reuse as if it were the original project**.

---

## UI

SilenceProtector provides a GUI for configuring major protection options.

- Input JAR
- Output JAR
- Main Class
- Class Name Obfuscation
- Method Name Obfuscation
- Field Name Obfuscation
- Package Randomization
- String Protection
- Number Protection
- Package Root
- Saved Settings

Previously used paths and options can be saved and restored automatically on the next launch.

---

## Build

```bash
gradle clean build
```

Build output:

```text
build/libs/SilenceProtector-1.0.0.jar
```

Run:

```bash
java -jar build/libs/SilenceProtector-1.0.0.jar
```

---

## Status

SilenceProtector is currently under development.

Obfuscation strategies, compatibility behavior, performance characteristics, and randomization logic may change over time.

---

## License

MIT License
