# forward»

Forward is a simple JVM language created to study and experiment with compiler construction.

**THIS IS AN EDUCATIONAL AND RESEARCH PROJECT NOT INTENDED FOR PRODUCTION USE.
MOST THINGS ARE BARELY WORKING OR OUTRIGHT BROKEN.**

## Getting Started

1. Install JDK 17+.
2. Run `./gradlew build` on Unixes or `gradlew build` on Windows.
3. Read `IDE.md` on how to set up your IDE for Forward.
4. Read `compiler/src/test/resources/forward/Syntax.fw` to familiarize yourself with language syntax.
5. Compile an example with `java -jar compiler/build/libs/compiler.jar HelloWorld.fw` and run `java HelloWorld`.
6. Read `compiler/src/test/resources/forward/Algorithms.fw` for more advanced examples.

## TODO

Features are listed in order of their priority.

- Support objects as targets in access expressions.
- Support usefully minimal set of operators.
- Full array support (create, access).
- Add automatic conversions for types supported only in Java.
- Decide on OOP approach (private/protected support, interfaces).
- Add break and continue.
- Write extensive feature tests.
- Templating support (using source text templates).
- Lambda support (by compiling source at runtime).
- Proper NaN treatment in comparison.

**IF YOU NOTICE THAT SOMETHING DOESN'T WORK, PLEASE CREATE AN ISSUE WITH DETAILS.**
