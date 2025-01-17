#!/bin/bash

# Check if the correct number of arguments is provided
if [ $# -ne 3 ]; then
    echo "Usage: $0 <filename> <port_number> <web_number>"
    exit 1
fi

# Assign the command-line arguments to variables
filename="$1"
port_number="$2"
web_number="$3"

# Check if the file exists
if [ ! -f "$filename" ]; then
    echo "File '$filename' does not exist."
    exit 1
fi

# Perform the replacement using sed
sed -i "s/___PORT___1/$port_number/g" "$filename"
sed -i "s/___PORT___2/$web_number/g" "$filename"