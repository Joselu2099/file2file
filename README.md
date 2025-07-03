# file2file

## Overview
**file2file** is a Java-based file conversion library, command-line tool, and simple graphical application.  
It currently supports:
- **CSH (C shell) to Bash (SH) script conversion**
- **Text file encoding conversion** (e.g., between UTF-8, ISO-8859-1, etc.)

You can use file2file as:
1. **A Java library** – Integrate it into your Java projects.
2. **A standalone executable JAR** – Use it from the command line.
3. **A simple GUI application** – Convert files with a graphical interface.

---

## Features

- Adds a `#!/bin/bash` shebang if missing (for CSH to SH).
- Converts common CSH constructs: `if`, `while`, `foreach`, `switch`, `alias`, `goto`.
- Preserves script formatting and indentation.
- Converts text file encodings between common formats.
- User-friendly GUI for selecting converter, file, and running the conversion.
- Output files are saved with `_CONVERTED` suffix and the appropriate extension.
- Available as a reusable **Java library**, **CLI tool**, and **GUI application**.

---

## Installation

You need **Java 17 or later** installed.

1. Download the latest `file2file.jar` from the [releases page](#) (replace with actual link).
2. To verify the installation, run:
   ```sh
   java -jar file2file.jar --help
   ```

---

## Usage

### 1. Command-Line Tool

#### Convert a `.csh` file to `.sh`:
```sh
java -jar file2file.jar -t sh path/to/script.csh
```
Output: `script_CONVERTED.sh`

#### Convert a text file encoding:
```sh
java -jar file2file.jar -t encoding path/to/file.txt
```
Output: `file_CONVERTED.txt`

#### Show help:
```sh
java -jar file2file.jar --help
```

---

### 2. Graphical User Interface (GUI)

Run the GUI with:
```sh
java -cp file2file.jar joselusc.libraries.file2file.gui.File2FileGUI
```
- Select the converter type (CSH to SH or Encoding).
- Browse and select the input file.
- Click "Convert" to generate the output file with `_CONVERTED` in the same directory.

---

### 3. Java Library

Add as a dependency (if published to Maven Central, update accordingly):

```xml
<dependency>
    <groupId>joselusc.libraries</groupId>
    <artifactId>file2file</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

#### Example Java Usage

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

---

## Development

To build from source:

```sh
git clone https://github.com/Joselu2099/file2file.git
cd file2file
mvn clean package
```

---

## Roadmap

Planned and possible future features:
- More file format conversions (e.g., Markdown to HTML, CSV to JSON, etc.)
- Advanced Bash compatibility improvements.
- Enhanced GUI with batch conversion and more options.
- Integration with cloud storage or web services.

---

## License

This project is licensed under the **MIT License** – see the [LICENSE](LICENSE) file for details.

---

## Contributions

Contributions, bug reports, and feature requests are welcome!  
Feel free to open an issue or submit a pull request.

---

## Author

**Jose Luis Sanchez Carrasco** – [GitHub](https://github.com/Joselu2099)