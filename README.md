# file2file

## Overview
**file2file** is a Java-based file conversion library and command-line tool. Currently, it supports converting **CSH (C shell) scripts to Bash (SH) scripts**. The conversion preserves indentation, blank lines, and key syntax transformations to ensure compatibility with Bash.

This library can be used in two ways:
1. **As a Java library** – You can integrate it into your Java projects and use it programmatically.
2. **As a standalone executable JAR** – You can run the tool from the command line to convert CSH files to SH files.

## Features
- Adds a `#!/bin/bash` shebang if missing.
- Converts common CSH constructs like `if`, `while`, `foreach`, `switch`, `alias`, and `goto`.
- Best-effort translation with structured pattern recognition.
- Preserves script formatting, making the transition to Bash smoother.
- Available as a reusable **Java library** and an **executable JAR**.

## Installation
To use `file2file` as a command-line tool, you need to have **Java 8 or later** installed.

1. Download the latest `file2file.jar` from the [releases page](#) (replace with actual link).
2. Run the following command to verify the installation:
   ```sh
   java -jar file2file.jar --help
   ```

## Usage

### **1. Using file2file as a Command-Line Tool**
To convert a `.csh` file to `.sh`, run:

```sh
java -jar file2file.jar -t sh path/to/script.csh
```

Example:
```sh
java -jar file2file.jar -t sh /home/user/script.csh
```
This will generate `/home/user/script_REFACTORED.sh` as output.

### **2. Using file2file as a Java Library**
You can integrate `file2file` into your Java project by adding it as a dependency (if hosted in Maven, update accordingly):

#### **Maven Dependency**
```xml
<dependency>
    <groupId>joselusc.libraries</groupId>
    <artifactId>file2file</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

#### **Example Java Code**
```java
import joselusc.libraries.file2file.Converter;
import joselusc.libraries.file2file.ConverterFactory;
import java.io.File;

public class FileConverterExample {
    public static void main(String[] args) throws Exception {
        String inputFilePath = "script.csh";
        Converter converter = ConverterFactory.getConverter(inputFilePath, "sh");
        File output = converter.convert(inputFilePath);
        System.out.println("Converted file: " + output.getAbsolutePath());
    }
}
```

## Development
To build the project from source, clone this repository and run:

```sh
git clone https://github.com/Joselu2099/file2file.git
cd file2file
mvn clean package
```

## Roadmap
Currently, `file2file` only supports **CSH to SH conversion**, but future releases may include:
- Additional file format conversions.
- Enhanced Bash compatibility improvements.
- GUI-based file converter.

## License
This project is licensed under the **MIT License** – see the [LICENSE](LICENSE) file for details.

## Contributions
Contributions, bug reports, and feature requests are welcome! Feel free to open an issue or submit a pull request.

## Author
**Jose Luis Sanchez Carrasco** – [GitHub](https://github.com/Joselu2099)