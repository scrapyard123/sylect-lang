# forward»

Forward is a simple JVM language created to study and experiment with compiler construction.

**THIS IS AN EDUCATIONAL AND RESEARCH PROJECT NOT INTENDED FOR PRODUCTION USE.
MOST THINGS ARE BARELY WORKING OR OUTRIGHT BROKEN.**

## Getting Started

Since language doesn't support arrays and method overloading yet, you'll need some bootstrap Java class with `main`
method and I/O helper methods, see `compiler/src/test/interop` and `compiler/src/test/java/forward/TestUtils.java`
for examples.

1. Install JDK 17+.
2. Run `./gradlew build` on Unixes or `gradlew build` on Windows.
3. Read `IDE.md` on how to set up your IDE for Forward.
4. Read `compiler/src/test/resources/forward/Syntax.fw` to familiarize yourself with language syntax.
5. Compile your program with `java -jar compiler/build/libs/compiler.jar YourSourceFile.fw`.

## TODO

Features are listed in order of their priority.

- Type checks (+overloading support) and conversions for method calls.
- String type support.
- Array support or alternative (will enable `main` method support).
- Write extensive feature tests.
- Support all operators.
- Add break and continue.
- Templating support (using source text templates).
- Lambda support (by compiling source at runtime).
- Syntax correctness tracking.
- Proper NaN treatment in comparison.
- Proper stack depth calculation and local variable count.
- Stackmap frame support.

**IF YOU NOTICE THAT SOMETHING DOESN'T WORK, PLEASE CREATE AN ISSUE WITH DETAILS.**
