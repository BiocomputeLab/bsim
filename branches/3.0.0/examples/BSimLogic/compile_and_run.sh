#!/bin/zsh

javac -cp .:../../lib/core.jar:../../lib/vecmath.jar:../../lib/objimport.jar:../../lib/bsim3.0.jar *.java 
java  -cp .:../../lib/core.jar:../../lib/vecmath.jar:../../lib/objimport.jar:../../lib/bsim3.0.jar BSimLogic
rm *.class
