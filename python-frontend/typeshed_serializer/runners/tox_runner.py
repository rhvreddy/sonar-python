import os
import sys
import subprocess
import hashlib
import logging
import argparse


# Define the path variables using os.path
CURRENT_PATH = os.path.dirname(__file__)
CHECKSUM_FILE = os.path.join(CURRENT_PATH, '../checksum')
SERIALIZER_PATH = os.path.join(CURRENT_PATH, '../serializer')
RESOURCES_FOLDER_PATH = os.path.join(CURRENT_PATH, '../resources')
BINARY_FOLDER_PATH = os.path.join(CURRENT_PATH, '../../src/main/resources/org/sonar/python/types')
PROTOBUF_EXTENSION = '.protobuf'
PYTHON_STUB_EXTENSION = '.pyi'
global compute_checksum
logger = logging.getLogger('tox_runner')
handler = logging.StreamHandler(sys.stdout)
log_formatter = logging.Formatter(fmt='%(name)s [%(levelname)s] --- %(message)s ---')
logger.setLevel(logging.INFO)
handler.setFormatter(log_formatter)
logger.addHandler(handler)

def fetch_python_file_names(folder_path):
    python_files = []
    for root, dirs, files in os.walk(folder_path):
        for file in files:
            if file.endswith('.py'):
                python_files.append(os.path.join(root, file))
    return python_files

def fetch_resource_file_names(folder_name, file_extension):
    matching_files = []
    for root, dirs, files in os.walk(folder_name):
        for file in files:
            if file.endswith(file_extension):
                matching_files.append(os.path.join(root, file))
    return matching_files

def fetch_config_file_names():
    return ['requirements.txt', 'tox.ini']

def fetch_binary_file_names():
    return sorted(fetch_resource_file_names(BINARY_FOLDER_PATH, PROTOBUF_EXTENSION))

def fetch_source_file_names(folder_path):
    filenames = fetch_python_file_names(folder_path)
    resources = fetch_resource_file_names(RESOURCES_FOLDER_PATH, PYTHON_STUB_EXTENSION)
    config_files = fetch_config_file_names()
    return sorted(filenames + resources + config_files)

def normalize_text_files(file_name):
    with open(file_name, 'r', encoding='utf-8') as file:
        normalized_file = file.read().strip().replace('\r\n', '\n').replace('\r', '\n')
    return normalized_file.encode('utf-8')

def read_file(file_name):
    with open(file_name, 'rb') as file:
        return file.read()


def update_checksum():
    with open(CHECKSUM_FILE, 'w') as file:
        source_file_names = fetch_source_file_names(SERIALIZER_PATH)
        source_checksum = compute_checksum(source_file_names, normalize_text_files)
        binary_file_names = fetch_binary_file_names()
        binary_checksum = compute_checksum(binary_file_names, read_file)
        file.write('{}\n{}'.format(source_checksum, binary_checksum))

def __log_process_begins(is_for_binary, over_n_files, previous_checksum, current_checksum):
    file_type = "BINARY" if is_for_binary else "SOURCE"
    binaries = "binaries " if is_for_binary else ""
    logger.info("STARTING TYPESHED {} FILE CHECKSUM COMPUTATION".format(file_type))
    logger.info("Previous {}checksum: {}".format(binaries, previous_checksum))
    logger.info("Current {}checksum: {}".format(binaries, current_checksum))
    logger.info("Checksum is computed over {} files".format(over_n_files))

def main(skip_tests=False, fail_fast=False):
    source_files = fetch_source_file_names(SERIALIZER_PATH)
    current_sources_checksum = compute_checksum(source_files, normalize_text_files)
    previous_sources_checksum, previous_binaries_checksum = read_previous_checksum(CHECKSUM_FILE)
    __log_process_begins(False, len(source_files), previous_sources_checksum, current_sources_checksum)
    if previous_sources_checksum != current_sources_checksum:
        if fail_fast:
            raise RuntimeError('INCONSISTENT SOURCES CHECKSUMS')
        else:
            logger.info("STARTING TYPESHED SERIALIZATION")
            try:
                subprocess.run(['python', '-m', 'tox'], check=True)
            except subprocess.CalledProcessError as e:
                logger.error("Subprocess failed with return code {}".format(e.returncode))
                raise
    else:
        binary_file_names = fetch_binary_file_names()
        current_binaries_checksum = compute_checksum(binary_file_names, read_file)
        __log_process_begins(True, len(binary_file_names), previous_binaries_checksum, current_binaries_checksum)
        if previous_binaries_checksum != current_binaries_checksum:
            raise RuntimeError('INCONSISTENT BINARY CHECKSUMS')
        logger.info("SKIPPING TYPESHED SERIALIZATION")
        if skip_tests:
            logger.info("SKIPPING TYPESHED SERIALIZER TESTS")
            return
        try:
            subprocess.run(['python', '-m', 'tox', '-e', 'py39'], check=True)
        except subprocess.CalledProcessError as e:
            logger.error("Subprocess failed with return code {}".format(e.returncode))
            raise

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--skip_tests', type=str, default="false")
    parser.add_argument('--fail_fast', type=str, default="false")
    args = parser.parse_args()
    skip_tests = args.skip_tests.lower() == "true"
    fail_fast = args.fail_fast.lower() == "true"
    main(skip_tests, fail_fast)
