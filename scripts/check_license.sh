#!/bin/bash
find . -name "*.java" | xargs grep -L "Copyright (c) 2017, Daniel Andersen (daniel@trollsahead.dk)"
find . -name "*.kt" | xargs grep -L "Copyright (c) 2017, Daniel Andersen (daniel@trollsahead.dk)"
