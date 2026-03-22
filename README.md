# project-test-ai

Java monorepo managed with **Apache Maven**.  
Each sub-module lives in its own directory and inherits common settings from the root parent POM.

## Repository structure

```
project-test-ai/          ← root (parent POM, packaging=pom)
├── pom.xml
├── module-one/           ← first Java sub-module
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/example/moduleone/
│       └── test/java/com/example/moduleone/
└── module-two/           ← second Java sub-module
    ├── pom.xml
    └── src/
        ├── main/java/com/example/moduletwo/
        └── test/java/com/example/moduletwo/
```

## Prerequisites

| Tool  | Minimum version |
|-------|-----------------|
| JDK   | 17              |
| Maven | 3.9+            |

## Build & test

```bash
# Build all modules
mvn package

# Run all tests
mvn verify

# Build a specific module only
mvn package -pl module-one
```

## Adding a new sub-module

1. Create the module directory and standard Maven source layout:
   ```bash
   mkdir -p my-module/src/{main,test}/java/com/example/mymodule
   ```
2. Add a `my-module/pom.xml` that declares the root POM as `<parent>`.
3. Register the module in the root `pom.xml` `<modules>` section:
   ```xml
   <module>my-module</module>
   ```
4. Run `mvn verify` to confirm everything builds and tests pass.