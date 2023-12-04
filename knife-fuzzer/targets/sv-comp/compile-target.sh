#!/bin/bash

# Check if at least two arguments are provided
if [ "$#" -lt 2 ]; then
    echo "Usage: $0 <classpath1> ... <target_file>"
    exit 1
fi

# Check if the fourth argument ends with 'rbtree' and swap if true
if [[ "${4}" == *rbtree ]]; then
    temp=$3
    set -- "${@:1:2}" "$4" "$temp" "${@:5}"
fi

# print all params
shift # Remove the first argument (property file)

# Extract the target file (last argument)
target_directory="${!#}"  # Get the last argument

# Construct the classpath using all remaining arguments except the last one
classpath=$(IFS=:; echo "${*:1:$#-1}/")

build_directory="$target_directory/build"

#echo $target_directory $classpath $build_directory
# Create the build directory if it doesn't exist
mkdir -p "$build_directory"

# Use find to get all .java files recursively from the target directory
java_files=$(find "$target_directory" -name "*.java")

echo "javac  -source 1.8 -target 1.8 -cp \"$classpath\" -d \"$build_directory\" $java_files"
# Compile all the found .java files
javac  -source 1.8 -target 1.8 -cp "$classpath" -d "$build_directory" $java_files
