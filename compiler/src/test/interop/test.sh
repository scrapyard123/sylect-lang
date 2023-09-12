#!/bin/bash
rm -f -- *.class

javac TestA.java
java -cp .:compiler.jar forward.bootstrap.BootstrapCompiler TestC.fw TestB.fw
javac -cp . TestD.java

java -cp . TestD

rm -f -- *.class
