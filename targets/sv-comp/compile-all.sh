#!/bin/bash

# Check if the args.txt file exists
if [ ! -f "args.txt" ]; then
    echo "args.txt file not found!"
    exit 1
fi

# Get the total number of lines in args.txt
total_lines=$(wc -l < args.txt)
current_line=0

# Function to print the progress bar
print_progress() {
    local percent=$((100 * $current_line / $total_lines))
    local filled=$((percent / 2))
    local empty=$((50 - filled))
    printf "\r["
    printf "%0.s#" $(seq 1 $filled)
    printf "%0.s-" $(seq 1 $empty)
    printf "] %d%%" $percent
}

# Iterate over each line in the args.txt file
while IFS= read -r args; do
    # Print progress
    print_progress

    # Replace all instances of ../../sv-benchmarks/java/
    modified_args=${args//..\/..\/sv-benchmarks\/java\//}

    # Call the compile--target.sh script with the modified arguments
    ./compile-target.sh $modified_args

    ((current_line++))
done < args.txt

# Print 100% completion at the end
print_progress
echo ""  # Move to the next line
