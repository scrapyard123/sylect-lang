#!/bin/bash
rm -f -- *.class

javac TestA.java
java -cp .:compiler.jar sylect.bootstrap.BootstrapCompiler TestC.sy TestB.sy
javac -cp . TestD.java

java -cp . TestD

rm -f -- *.class
