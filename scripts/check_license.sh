#!/bin/bash
find . -name "*.java" | xargs grep -L "Copyright (c) 2015, Daniel Andersen (daniel@trollsahead.dk)"
