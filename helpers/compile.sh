#!/bin/bash

modulepath=../lib:../artifacts 
classpath=../java
filepath="$classpath/ru/ifmo/rain/naumkin"

javac -cp $classpath -p $modulepath "$filepath/$1"
