import os

def find_verdicts_in_yml(directory_path):
    verdicts = []

    for root, dirs, files in os.walk(directory_path):
        for file in files:
            if file.endswith('.yml'):
                full_path = os.path.join(root, file)
                with open(full_path, 'r') as f:
                    for line in f:
                        if 'expected_verdict:' in line:
                            verdicts.append(f"{full_path}: {line.strip()}")
                            break
    return verdicts

def save_verdicts_to_file(verdicts, output_file):
    with open(output_file, 'w') as f:
        for verdict in verdicts:
            f.write(f"{verdict}\n")

directory_path = '.'  # Replace with the path to your directory
output_file_path = './verdicts.txt'  # Replace with the desired path for your output file

verdicts = find_verdicts_in_yml(directory_path)
save_verdicts_to_file(verdicts, output_file_path)
