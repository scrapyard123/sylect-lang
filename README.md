# forward»

**THIS IS AN EXPERIMENTAL PROJECT NOT INTENDED FOR PRODUCTION USE.**

Forward is a simple JVM language which strives to be as close to JVM functionality as possible
without compromising ease of use. The primary goal of this language is to serve as a base for
language construction experiments through use of compiler extensions (TODO).

## Getting Started

1. Install JDK 17+ and Maven 3.x.
2. Run `mvn install` to build and install locally project components.
3. Read `IDE.md` on how to set up your IDE for Forward.
4. Read `compiler/src/test/resources/forward/Syntax.fw` to familiarize yourself with language syntax.
5. Compile an example with `java -jar compiler/target/compiler-0.1.jar HelloWorld.fw` and run `java HelloWorld`.
6. Read `compiler/src/test/resources/forward/Algorithms.fw` for more advanced examples.
7. Take a look at `stdlib` which is an example of how you can set up your Forward projects.

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

## TODO

Features are listed in order of their priority.

- Add compiler extension support.
- Rework access expressions (potentially merge with usual expressions).
- Support usefully minimal set of operators.
- Add break and continue.
- Full annotation support.
- Decide on OOP approach (private/protected support, interfaces).
- Full array support (create, access).
- Add automatic conversions for types supported only in Java.
- Write extensive feature tests.
- Add various algorithms/data structures to `stdlib`.
- Proper NaN treatment in comparison.
