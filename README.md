# forward»

**THIS IS AN EXPERIMENTAL PROJECT NOT INTENDED FOR PRODUCTION USE.**

Forward is a simple JVM language which strives to be as close to JVM functionality as possible
without compromising ease of use. The primary goal of this language is to serve as a base for
language design experiments.

## Getting Started

1. Install JDK 17+ and Maven 3.x.
2. Run `mvn install` to build and install locally project components.
3. Read `IDE.md` on how to set up your IDE for Forward.
4. Read `compiler/src/test/resources/forward/ClassSyntax.fw` to familiarize yourself with language syntax.
5. Compile an example with `java -jar compiler/target/compiler-0.1.jar HelloWorld.fw` and run `java HelloWorld`.
6. Take a look at `example` for various examples.
7. Take a look at https://github.com/scrapyard123/eclinic for Spring Boot application example with Spring Data JPA.

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
                <artifactId>forward-maven-plugin</artifactId>
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

| Feature                                                 | MVP                | Tested     |
|---------------------------------------------------------|--------------------|------------|
| **Class Structure Definition**                          | :heavy_check_mark: | :notebook: |
| **»** File Structure                                    | :heavy_check_mark: | :notebook: |
| **»** Class Definition                                  | :heavy_check_mark: | :notebook: |
| **»** Abstract Class Definition                         | :x:                | :x:        |
| **»** Interface Definition                              | :heavy_check_mark: | :notebook: |
| **»** Annotation Definition                             | :x:                | :x:        |
| **»** Enum Definition                                   | :x:                | :x:        |
| **»** Inheritance & Interface Implementation            | :heavy_check_mark: | :notebook: |
| **Access Modifiers**                                    | :construction:     | :notebook: |
| **»** Protected/Private Fields/Methods                  | :notebook:         | :notebook: |
| **»** Package-Private Fields/Methods                    | :x:                | :x:        |
| **»** Static Fields/Methods                             | :heavy_check_mark: | :notebook: |
| **»** Final Fields (+ for Interfaces)                   | :x:                | :x:        |
| **»** Final Methods/Classes/etc                         | :x:                | :x:        |
| **Annotations**                                         | :heavy_check_mark: | :notebook: |
| **»** Annotations for Classes/Fields/Methods/Parameters | :heavy_check_mark: | :notebook: |
| **»** Annotations for Local Variables/etc               | :x:                | :x:        |
| **»** Parameter-less Annotations                        | :heavy_check_mark: | :notebook: |
| **»** Annotation Parameters                             | :heavy_check_mark: | :notebook: |
| **»** Array Parameters                                  | :heavy_check_mark: | :question: |
| **Statement Types**                                     | :construction:     | :notebook: |
| **»** Variable Definition                               | :heavy_check_mark: | :notebook: |
| **»** Assignment                                        | :heavy_check_mark: | :notebook: |
| **»** Standalone Expression                             | :heavy_check_mark: | :notebook: |
| **»** Conditional                                       | :heavy_check_mark: | :notebook: |
| **»** Looping                                           | :heavy_check_mark: | :notebook: |
| **»** Return                                            | :heavy_check_mark: | :notebook: |
| **»** Break/Continue                                    | :heavy_check_mark: | :notebook: |
| **»** Try/Catch/Throw                                   | :question:         | :notebook: |
| **»** Switch/Case or When                               | :x:                | :x:        |
| **Variable Definition**                                 | :heavy_check_mark: | :notebook: |
| **»** Scope Support                                     | :x:                | :x:        |
| **Assignment Statement**                                | :heavy_check_mark: | :notebook: |
| **»** Local Variable Assignment                         | :heavy_check_mark: | :notebook: |
| **»** Same-Object Field Assignment                      | :heavy_check_mark: | :notebook: |
| **»** General Field Assignment                          | :x:                | :x:        |
| **»** Array Assignment                                  | :x:                | :x:        |
| **Expressions**                                         | :construction:     | :notebook: |
| **»** Numerical Literals                                | :heavy_check_mark: | :notebook: |
| **»** String Literals                                   | :heavy_check_mark: | :notebook: |
| **»** Class Literals                                    | :recycle:          | :notebook: |
| **»** Field/Variable Access                             | :recycle:          | :notebook: |
| **»** Object Instantiation                              | :notebook:         | :notebook: |
| **»** Array Instantiation                               | :x:                | :x:        |
| **»** null                                              | :x:                | :x:        |
| **»** Mathematical Operators                            | :heavy_check_mark: | :notebook: |
| **»** Comparison Operators                              | :heavy_check_mark: | :notebook: |
| **»** Proper NaN Treatment During Comparison            | :question:         | :question: |
| **»** Logical Operators                                 | :notebook:         | :notebook: |
| **»** Array Operators                                   | :x:                | :x:        |
| **»** Method Calls                                      | :recycle:          | :notebook: |
| **»** Constructor Calls                                 | :notebook:         | :notebook: |
| **»** Type Conversion                                   | :heavy_check_mark: | :notebook: |
| **Method Calls**                                        | :heavy_check_mark: | :notebook: |
| **»** Basic Method Calls                                | :heavy_check_mark: | :notebook: |
| **»** Overloaded Method Calls                           | :heavy_check_mark: | :notebook: |
| **»** Unsupported Type Conversion                       | :x:                | :x:        |
| **Conditional Statement**                               | :heavy_check_mark: | :notebook: |
| **Loop Statements**                                     | :heavy_check_mark: | :notebook: |
| **»** While Loop                                        | :heavy_check_mark: | :notebook: |
| **»** For Loop                                          | :heavy_check_mark: | :notebook: |
| **»** Do-While Loop                                     | :x:                | :x:        |
| **Return Statement**                                    | :heavy_check_mark: | :notebook: |
| **»** Return Statements with Expressions                | :heavy_check_mark: | :notebook: |
| **»** Add Automatically to Void Methods                 | :heavy_check_mark: | :notebook: |
| **»** Guard from Code Paths Without Return              | :x:                | :x:        |
| **Infrastructure**                                      | :heavy_check_mark: | :notebook: |
| **»** Maven Plugin                                      | :heavy_check_mark: | :notebook: |
| **»** Testing Support in Maven Plugin                   | :heavy_check_mark: | :notebook: |
| **»** Algorithms & Data Structures                      | :question:         | :question: |
