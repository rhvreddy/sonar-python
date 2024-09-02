import contextlib
import os
import sys
from os.path import isfile, join
import subprocess
import hashlib
from pathlib import Path
from typing import Optional, Tuple, Union
from collections.abc import Callable
import logging
import argparse

CURRENT_PATH = Path(__file__).parent
CHECKSUM_FILE = CURRENT_PATH / '../checksum'
SERIALIZER_PATH = CURRENT_PATH / '../serializer'
RESOURCES_FOLDER_PATH = CURRENT_PATH / '../resources'
BINARY_FOLDER_PATH = CURRENT_PATH / '../../src/main/resources/org/sonar/python/types'
PROTOBUF_EXTENSION = '.protobuf'
PYTHON_STUB_EXTENSION = '.pyi'

logger = logging.getLogger('tox_runner')
handler = logging.StreamHandler(sys.stdout)
log_formatter = logging.Formatter(fmt='%(name)s [%(levelname)s] --- %(message)s ---')
logger.setLevel(logging.INFO)
handler.setFormatter(log_formatter)
logger.addHandler(handler)

def fetch_python_file_names(folder_path: Path) -> list[str]:
    return [str(file) for file in folder_path.glob('*.py')]

def fetch_resource_file_names(folder_name: Path, file_extension: str) -> list[str]:
    return [str(file) for file in folder_name.rglob(f'*{file_extension}')]

def fetch_config_file_names() -> list[str]:
    return ['requirements.txt', 'tox.ini']

def fetch_binary_file_names() -> list[str]:
    return sorted(fetch_resource_file_names(BINARY_FOLDER_PATH, PROTOBUF_EXTENSION))

def fetch_source_file_names(folder_path: Path) -> list[str]:
    filenames = fetch_python_file_names(folder_path)
    resources = fetch_resource_file_names(RESOURCES_FOLDER_PATH, PYTHON_STUB_EXTENSION)
    config_files = fetch_config_file_names()
    return sorted(filenames + resources + config_files)

def normalize_text_files(file_name: str) -> bytes:
    normalized_file = Path(file_name).read_text().strip().replace('\r\n', '\n').replace('\r', '\n')
    return bytes(normalized_file, 'utf-8')

def read_file(file_name: str) -> bytes:
    return Path(file_name).read_bytes()

def compute_checksum(file_names: list[str], get_file_bytes: Callable[[str], bytes]) -> str:
    _hash = hashlib.sha256()
    for fn in file_names:
        with contextlib.suppress(IsADirectoryError):
            _hash.update(get_file_bytes(fn))
    return _hash.hexdigest()

def read_previous_checksum(checksum_file: Path) -> Tuple[Optional[str], Optional[str]]:
    if not checksum_file.is_file():
        return None, None
    with checksum_file.open('r') as file:
        source_checksum = file.readline().strip() or None
        binaries_checksum = file.readline().strip() or None
        return source_checksum, binaries_checksum

def update_checksum():
    with CHECKSUM_FILE.open('w') as file:
        source_file_names = fetch_source_file_names(SERIALIZER_PATH)
        source_checksum = compute_checksum(source_file_names, normalize_text_files)
        binary_file_names = fetch_binary_file_names()
        binary_checksum = compute_checksum(binary_file_names, read_file)
        file.write(f"{source_checksum}\n{binary_checksum}")

def __log_process_begins(is_for_binary: bool, over_n_files: int, previous_checksum: Union[str, None], current_checksum: str) -> None:
    file_type = "BINARY" if is_for_binary else "SOURCE"
    binaries = "binaries " if is_for_binary else ""
    logger.info(f"STARTING TYPESHED {file_type} FILE CHECKSUM COMPUTATION")
    logger.info(f"Previous {binaries}checksum: {previous_checksum}")
    logger.info(f"Current {binaries}checksum: {current_checksum}")
    logger.info(f"Checksum is computed over {over_n_files} files")

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
                logger.error(f"Subprocess failed with return code {e.returncode}")
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
            logger.error(f"Subprocess failed with return code {e.returncode}")
            raise

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--skip_tests', type=str, default="false")
    parser.add_argument('--fail_fast', type=str, default="false")
    args = parser.parse_args()
    skip_tests = args.skip_tests.lower() == "true"
    fail_fast = args.fail_fast.lower() == "true"
    main(skip_tests, fail_fast)
