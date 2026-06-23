# Pipeline de CI/CD de Calidad y Entregas Automáticas Plan de Implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a modular CI/CD pipeline using GitHub Actions, Checkstyle, and SpotBugs to enforce code quality, run tests, and publish automated releases upon maven version updates.

**Architecture:** Split validation and delivery into two workflows: `ci.yml` (triggered on pull requests and commits to validate code quality and run tests) and `release.yml` (triggered on push to `main` to build, tag, and publish the jar with checksums if a version increment is found). Integrated Checkstyle and SpotBugs plugins directly into the Maven build cycle.

**Tech Stack:** GitHub Actions, Maven, Checkstyle, SpotBugs.

---

## 🛠️ Files to be Created or Modified
* **Create**: `checkstyle.xml` (rules configuration in project root)
* **Create**: `.github/workflows/ci.yml` (CI pipeline)
* **Create**: `.github/workflows/release.yml` (CD pipeline)
* **Modify**: `pom.xml` (register Checkstyle and SpotBugs plugins)
* **Modify**: Java source files as needed to resolve checkstyle/spotbugs linting violations.

---

### Task 1: Create Checkstyle Rules Configuration

**Files:**
- Create: `checkstyle.xml`

- [ ] **Step 1: Write checkstyle.xml config in the root directory**

Write the rules configuration file to validate line length (120 chars), forbid star imports (`import java.util.*`), check naming, bracket styles, and prohibit the use of `System.out.println` or `System.err.println`.

```xml
<?xml version="1.0"?>
<!DOCTYPE module PUBLIC
          "-//Checkstyle//DTD Checkstyle Configuration 1.3//EN"
          "https://checkstyle.org/dtds/configuration_1_3.dtd">

<module name="Checker">
    <property name="charset" value="UTF-8"/>
    <property name="severity" value="error"/>

    <module name="LineLength">
        <property name="max" value="120"/>
        <property name="ignorePattern" value="^package.*|^import.*|a href|href|http://|https://"/>
    </module>

    <module name="TreeWalker">
        <module name="AvoidStarImport"/>
        <module name="UnusedImports"/>
        <module name="RedundantImport"/>

        <module name="TypeName"/>
        <module name="MemberName"/>
        <module name="MethodName"/>
        <module name="ParameterName"/>
        <module name="LocalVariableName"/>
        <module name="ConstantName"/>

        <module name="LeftCurly"/>
        <module name="RightCurly"/>
        <module name="NeedBraces"/>

        <module name="EmptyBlock"/>

        <module name="RegexpSinglelineJava">
            <property name="format" value="System\.(out|err)\.print"/>
            <property name="message" value="Usa Logger en lugar de System.out o System.err para imprimir en consola."/>
            <property name="ignoreComments" value="true"/>
        </module>
    </module>
</module>
```

- [ ] **Step 2: Verify the file is created**

Verify that `checkstyle.xml` exists in the root directory of the workspace.

- [ ] **Step 3: Commit**

```bash
git add checkstyle.xml
git commit -m "style: add checkstyle.xml configuration rules"
```

---

### Task 2: Configure Maven Plugins for Quality Verification

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: Add Checkstyle and SpotBugs plugins inside the plugins block of build**

Add `maven-checkstyle-plugin` and `spotbugs-maven-plugin` configuration blocks under `<build><plugins>` in `pom.xml`.

Target block:
```xml
        <plugins>
            <!-- Plugin para incluir dependencias dentro del JAR -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.4.1</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            <!-- CHECKSTYLE PLUGIN -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-checkstyle-plugin</artifactId>
                <version>3.3.1</version>
                <configuration>
                    <configLocation>checkstyle.xml</configLocation>
                    <consoleOutput>true</consoleOutput>
                    <failsOnError>true</failsOnError>
                    <linkXRef>false</linkXRef>
                </configuration>
                <executions>
                    <execution>
                        <id>validate</id>
                        <phase>validate</phase>
                        <goals>
                            <goal>check</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            <!-- SPOTBUGS PLUGIN -->
            <plugin>
                <groupId>com.github.spotbugs</groupId>
                <artifactId>spotbugs-maven-plugin</artifactId>
                <version>4.8.3.1</version>
                <configuration>
                    <effort>Max</effort>
                    <threshold>Medium</threshold>
                    <failOnError>true</failOnError>
                </configuration>
                <executions>
                    <execution>
                        <id>analyze-compile</id>
                        <phase>verify</phase>
                        <goals>
                            <goal>check</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
```

- [ ] **Step 2: Run verify command to confirm plugins run (expect compile failures due to wildcard imports)**

Run: `mvn clean verify`
Expected: BUILD FAILURE with Checkstyle error messages about wildcard imports (e.g. `AvoidStarImport` violations).

- [ ] **Step 3: Commit pom.xml modifications**

```bash
git add pom.xml
git commit -m "build: register checkstyle and spotbugs plugins in maven build cycle"
```

---

### Task 3: Resolve Code Formatting and Quality Violations

**Files:**
- Modify: `src/main/java/joselusc/libraries/file2file/gui/File2FileGUI.java`
- Modify: `src/main/java/joselusc/libraries/file2file/converters/EncodingConverter.java`
- Modify: `src/main/java/joselusc/libraries/file2file/converters/AbstractConverter.java`
- Modify: `src/test/java/joselusc/libraries/file2file/converters/AbstractConverterTest.java`

- [ ] **Step 1: Replace wildcard imports in File2FileGUI.java**

Replace `import javax.swing.*;`, `import java.awt.*;`, `import java.awt.event.ActionEvent;`, `import java.awt.dnd.*;`, `import java.util.*;` with precise, explicit imports.

```java
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JProgressBar;
import javax.swing.UIManager;
import javax.swing.Box;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.BoxLayout;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Font;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DnDConstants;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.List;
import java.util.ArrayList;
```

- [ ] **Step 2: Replace wildcard imports in EncodingConverter.java**

Replace wildcard imports in `EncodingConverter.java` (lines 3-6) with explicit ones:

```java
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
```

- [ ] **Step 3: Replace wildcard imports in AbstractConverterTest.java**

Replace wildcard imports in `AbstractConverterTest.java` (lines 9-12):

```java
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
```

- [ ] **Step 4: Run verify command to confirm quality passes**

Run: `mvn clean verify`
Expected: BUILD SUCCESS (Checkstyle and SpotBugs pass without errors).

- [ ] **Step 5: Commit style/formatting fixes**

```bash
git add src/main/java/joselusc/libraries/file2file/gui/File2FileGUI.java src/main/java/joselusc/libraries/file2file/converters/EncodingConverter.java src/test/java/joselusc/libraries/file2file/converters/AbstractConverterTest.java
git commit -m "style: remove wildcard imports and align formatting with checkstyle rules"
```

---

### Task 4: Implement CI Workflow

**Files:**
- Create: `.github/workflows/ci.yml`

- [ ] **Step 1: Write ci.yml workflow file**

Write the CI configuration file to check out code, setup Java 17, cache maven dependencies, and run `mvn clean verify` on Pull Requests and commits to branch.

```yaml
name: Java CI with Maven Quality

on:
  push:
    branches-ignore: [ main, master ]
  pull_request:
    branches: [ main, master ]

permissions:
  contents: read

jobs:
  validate:
    runs-on: ubuntu-latest

    steps:
    - name: Checkout code
      uses: actions/checkout@v4

    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: 'maven'

    - name: Verify Quality and Run Tests
      run: mvn -B clean verify
```

- [ ] **Step 2: Verify the file is created**

Ensure that `.github/workflows/ci.yml` exists.

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/ci.yml
git commit -m "ci: add modular CI pipeline for compile, testing, and quality verification"
```

---

### Task 5: Implement CD Workflow

**Files:**
- Create: `.github/workflows/release.yml`

- [ ] **Step 1: Write release.yml CD workflow**

Create `.github/workflows/release.yml` to run on pushes to `main`. It will read the `pom.xml` version, verify if a tag already exists, compile the package if it's new, rename it, generate checksums, and publish a GitHub Release with the tag.

```yaml
name: Java CD Release and Tagging

on:
  push:
    branches: [ main, master ]

permissions:
  contents: write

jobs:
  release:
    runs-on: ubuntu-latest

    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      with:
        fetch-depth: 0

    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: 'maven'

    - name: Extract Version
      id: get_version
      run: |
        VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
        echo "version=$VERSION" >> $GITHUB_OUTPUT
        echo "Extracted project version: $VERSION"

    - name: Check if Tag Exists
      id: check_tag
      run: |
        TAG_NAME="v${{ steps.get_version.outputs.version }}"
        if git rev-parse "$TAG_NAME" >/dev/null 2>&1; then
          echo "tag_exists=true" >> $GITHUB_OUTPUT
          echo "Tag $TAG_NAME already exists. Skipping release."
        else
          echo "tag_exists=false" >> $GITHUB_OUTPUT
          echo "Tag $TAG_NAME does not exist. Proceeding with release."
        fi

    - name: Build Production Package
      if: steps.check_tag.outputs.tag_exists == 'false'
      run: mvn -B clean package -DskipTests

    - name: Rename JAR and Generate SHA-256 Checksum
      if: steps.check_tag.outputs.tag_exists == 'false'
      run: |
        VERSION="${{ steps.get_version.outputs.version }}"
        # Find the shaded fat JAR generated by maven-shade-plugin
        ORIGINAL_JAR=$(find target -name "file2file-${VERSION}.jar")
        RELEASE_JAR="target/file2file-v${VERSION}.jar"
        cp "$ORIGINAL_JAR" "$RELEASE_JAR"
        sha256sum "$RELEASE_JAR" > "${RELEASE_JAR}.sha256"
        echo "Generated release assets:"
        ls -la target/file2file-v*

    - name: Create GitHub Release
      if: steps.check_tag.outputs.tag_exists == 'false'
      uses: softprops/action-gh-release@v2
      with:
        tag_name: v${{ steps.get_version.outputs.version }}
        name: Release v${{ steps.get_version.outputs.version }}
        draft: false
        prerelease: false
        files: |
          target/file2file-v${{ steps.get_version.outputs.version }}.jar
          target/file2file-v${{ steps.get_version.outputs.version }}.jar.sha256
      env:
        GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

- [ ] **Step 2: Verify the file is created**

Ensure that `.github/workflows/release.yml` exists.

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/release.yml
git commit -m "cd: add automatic tag check, release assembly, and github release publishing"
```
