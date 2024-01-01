# forward»

**THIS IS AN EXPERIMENTAL PROJECT NOT INTENDED FOR PRODUCTION USE.**

Forward is a simple JVM language which strives to be as close to JVM functionality as possible
without compromising ease of use. The primary goal of this language is to serve as a base for
language construction experiments through use of compiler extensions (TODO).

## Getting Started

1. Install JDK 17+ and Maven 3.x.
2. Run `mvn install` to build and install locally project components.
3. Read `IDE.md` on how to set up your IDE for Forward.
4. Read `compiler/src/test/resources/forward/ClassSyntax.fw` to familiarize yourself with language syntax.
5. Compile an example with `java -jar compiler/target/compiler-0.1.jar HelloWorld.fw` and run `java HelloWorld`.
6. Take a look at `example` for various examples.

### Maven Plugin
You can apply Maven Plugin to write your projects in Forward.
Place your sources in `src/{main,test}/forward` and add the following to `pom.xml`:

```xml
<project>
    ...
    <properties>
        <forward.target>Target JVM version (e.g. 17)</forward.target>
    </properties>
    ...
    <build>
        <sourceDirectory>src/main/forward</sourceDirectory>
        <testSourceDirectory>src/test/forward</testSourceDirectory>

        <plugins>
            <plugin>
                <groupId>forward</groupId>
                <artifactId>maven-plugin</artifactId>
                <version>0.1</version>
                <executions>
                    <execution>
                        <id>compile</id>
                        <phase>compile</phase>
                        <goals>
                            <goal>compile-forward</goal>
                        </goals>
                    </execution>
                    <execution>
                        <id>test-compile</id>
                        <phase>test-compile</phase>
                        <goals>
                            <goal>test-compile-forward</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

## Feature Implementation State

| Feature                                          | MVP                | Tested     |
|--------------------------------------------------|--------------------|------------|
| **Class Structure Definition**                   | :heavy_check_mark: | :notebook: |
| **»** File Structure                             | :heavy_check_mark: | :notebook: |
| **»** Class Definition                           | :heavy_check_mark: | :notebook: |
| **»** Abstract Class Definition                  | :x:                | :x:        |
| **»** Interface Definition                       | :heavy_check_mark: | :notebook: |
| **»** Annotation Definition                      | :x:                | :x:        |
| **»** Enum Definition                            | :x:                | :x:        |
| **»** Inheritance & Interface Implementation     | :heavy_check_mark: | :notebook: |
| **Access Modifiers**                             | :construction:     | :notebook: |
| **»** Public/Private Fields/Methods              | :construction:     | :notebook: |
| **»** Protected Fields/Methods                   | :question:         | :question: |
| **»** Package-Private Fields/Methods             | :x:                | :x:        |
| **»** Static Fields/Methods                      | :heavy_check_mark: | :notebook: |
| **»** Final Fields (+ for Interfaces)            | :question:         | :question: |
| **»** Final Methods/Classes/etc                  | :x:                | :x:        |
| **Annotations**                                  | :construction:     | :notebook: |
| **»** Annotations for Classes/Methods/Parameters | :construction:     | :notebook: |
| **»** Annotations for Local Variables/etc        | :x:                | :x:        |
| **»** Parameter-less Annotations                 | :heavy_check_mark: | :notebook: |
| **»** Annotation Parameters                      | :notebook:         | :notebook: |
| **Statement Types**                              | :construction:     | :notebook: |
| **»** Variable Definition                        | :heavy_check_mark: | :notebook: |
| **»** Assignment                                 | :heavy_check_mark: | :notebook: |
| **»** Standalone Expression                      | :heavy_check_mark: | :notebook: |
| **»** Conditional                                | :heavy_check_mark: | :notebook: |
| **»** Looping                                    | :heavy_check_mark: | :notebook: |
| **»** Return                                     | :heavy_check_mark: | :notebook: |
| **»** Break/Continue                             | :notebook:         | :notebook: |
| **Variable Definition**                          | :construction:     | :notebook: |
| **»** Scope Support                              | :x:                | :x:        |
| **Assignment Statement**                         | :construction:     | :notebook: |
| **»** Local Variable Assignment                  | :heavy_check_mark: | :notebook: |
| **»** Same-Object Field Assignment               | :notebook:         | :notebook: |
| **»** General Field Assignment                   | :notebook:         | :notebook: |
| **»** Array Assignment                           | :notebook:         | :notebook: |
| **Expressions**                                  | :construction:     | :notebook: |
| **»** Numerical Literals                         | :heavy_check_mark: | :notebook: |
| **»** String Literals                            | :heavy_check_mark: | :notebook: |
| **»** Class Literals                             | :notebook:         | :notebook: |
| **»** Field/Variable Access                      | :recycle:          | :notebook: |
| **»** Object Instantiation                       | :notebook:         | :notebook: |
| **»** Array Instantiation                        | :notebook:         | :notebook: |
| **»** null                                       | :notebook:         | :notebook: |
| **»** Mathematical Operators                     | :heavy_check_mark: | :notebook: |
| **»** Comparison Operators                       | :heavy_check_mark: | :notebook: |
| **»** Proper NaN Treatment During Comparison     | :question:         | :question: |
| **»** Logical Operators                          | :notebook:         | :notebook: |
| **»** Array Operators                            | :notebook:         | :notebook: |
| **»** Method Calls                               | :recycle:          | :notebook: |
| **»** Constructor Calls                          | :notebook:         | :notebook: |
| **»** Type Conversion                            | :notebook:         | :notebook: |
| **Method Calls**                                 | :recycle:          | :notebook: |
| **»** Basic Method Calls                         | :heavy_check_mark: | :notebook: |
| **»** Overloaded Method Calls                    | :heavy_check_mark: | :notebook: |
| **»** Unsupported Type Conversion                | :heavy_check_mark: | :notebook: |
| **Conditional Statement**                        | :heavy_check_mark: | :notebook: |
| **Loop Statements**                              | :heavy_check_mark: | :notebook: |
| **»** While Loop                                 | :heavy_check_mark: | :notebook: |
| **»** For Loop                                   | :x:                | :x:        |
| **»** Do-While Loop                              | :x:                | :x:        |
| **Return Statement**                             | :construction:     | :notebook: |
| **»** Return Statements with Expressions         | :heavy_check_mark: | :notebook: |
| **»** Add Automatically to Void Methods          | :notebook:         | :notebook: |
| **Infrastructure**                               | :construction:     | :notebook: |
| **»** Maven Plugin                               | :heavy_check_mark: | :notebook: |
| **»** Testing Support in Maven Plugin            | :heavy_check_mark: | :notebook: |
| **»** Compiler Extensions                        | :notebook:         | :notebook: |
| **»** Algorithms & Data Structures               | :question:         | :question: |
