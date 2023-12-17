# forward»

Forward is a simple JVM language created to study and experiment with compiler construction.

**THIS IS A RESEARCH PROJECT NOT INTENDED FOR PRODUCTION USE.**

## Getting Started

1. Install JDK 17+ and Maven 3.x.
2. Run `mvn install` to build and install locally project components.
3. Read `IDE.md` on how to set up your IDE for Forward.
4. Read `compiler/src/test/resources/forward/Syntax.fw` to familiarize yourself with language syntax.
5. Compile an example with `java -jar compiler/target/compiler-0.1.jar HelloWorld.fw` and run `java HelloWorld`.
6. Read `compiler/src/test/resources/forward/Algorithms.fw` for more advanced examples.

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
                        <phase>compile</phase>
                        <goals>
                            <goal>compile-forward</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

## TODO

Features are listed in order of their priority.

- Support objects as targets in access expressions.
- Support usefully minimal set of operators.
- Full array support (create, access).
- Add automatic conversions for types supported only in Java.
- Decide on OOP approach (private/protected support, interfaces).
- Add break and continue.
- Add annotation support.
- Add testing support to Maven plugin.
- Write extensive feature tests.
- Proper NaN treatment in comparison.
