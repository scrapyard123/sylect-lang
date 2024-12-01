#!/bin/bash
rm -f -- *.class

javac TestA.java
java -cp .:compiler.jar sylect.SylectCompilerRunner TestC.sy TestB.sy
javac -cp . TestD.java

# Should output four fours
java -cp . TestD

rm -f -- *.class
