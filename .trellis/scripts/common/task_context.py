#!/usr/bin/env python3
"""
Task JSONL context management.

Provides:
    cmd_init_context  - Initialize JSONL context files for a task
    cmd_add_context   - Add entry to JSONL context file
    cmd_validate      - Validate JSONL context files
    cmd_list_context  - List JSONL context entries
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

from .cli_adapter import get_cli_adapter_auto
from .config import (
    get_packages,
    is_monorepo,
    resolve_package,
    validate_package,
)
from .io import read_json, write_json
from .log import Colors, colored
from .paths import (
    DIR_SPEC,
    DIR_WORKFLOW,
    FILE_TASK_JSON,
    get_repo_root,
)
from .task_utils import resolve_task_dir


# =============================================================================
# JSONL Default Content Generators
# =============================================================================

def get_implement_base() -> list[dict]:
    """Get base implement context entries."""
    return [
        {"file": f"{DIR_WORKFLOW}/workflow.md", "reason": "Project workflow and conventions"},
    ]


def get_implement_backend(package: str | None = None) -> list[dict]:
    """Get backend implement context entries."""
    spec_base = f"{DIR_SPEC}/{package}" if package else DIR_SPEC
    return [
        {"file": f"{DIR_WORKFLOW}/{spec_base}/backend/index.md", "reason": "Backend development guide"},
    ]


def get_implement_frontend(package: str | None = None) -> list[dict]:
    """Get frontend implement context entries."""
    spec_base = f"{DIR_SPEC}/{package}" if package else DIR_SPEC
    return [
        {"file": f"{DIR_WORKFLOW}/{spec_base}/frontend/index.md", "reason": "Frontend development guide"},
    ]


def get_check_context(repo_root: Path) -> list[dict]:
    """Get check context entries."""
    adapter = get_cli_adapter_auto(repo_root)

    entries = [
        {"file": adapter.get_trellis_command_path("finish-work"), "reason": "Finish work checklist"},
        {"file": adapter.get_trellis_command_path("check"), "reason": "Code quality check spec"},
    ]

    return entries


def get_debug_context(repo_root: Path) -> list[dict]:
    """Get debug context entries."""
    adapter = get_cli_adapter_auto(repo_root)

    entries: list[dict] = [
        {"file": adapter.get_trellis_command_path("check"), "reason": "Code quality check spec"},
    ]

    return entries


def _write_jsonl(path: Path, entries: list[dict]) -> None:
    """Write entries to JSONL file."""
    lines = [json.dumps(entry, ensure_ascii=False) for entry in entries]
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


# =============================================================================
# Command: init-context
# =============================================================================

def cmd_init_context(args: argparse.Namespace) -> int:
    """Initialize JSONL context files for a task."""
    repo_root = get_repo_root()
    target_dir = resolve_task_dir(args.dir, repo_root)
    dev_type = args.type

    if not dev_type:
        print(colored("Error: Missing arguments", Colors.RED))
        print("Usage: python3 task.py init-context <task-dir> <dev_type>")
        print("  dev_type: backend | frontend | fullstack | test | docs")
        return 1

    if not target_dir.is_dir():
        print(colored(f"Error: Directory not found: {target_dir}", Colors.RED))
        return 1

    # Resolve package: --package CLI → task.json.package → default_package
    cli_package: str | None = getattr(args, "package", None)
    package: str | None = None
    if not is_monorepo(repo_root):
        # Single-repo: ignore --package, no package prefix
        if cli_package:
            print(colored("Warning: --package ignored in single-repo project", Colors.YELLOW), file=sys.stderr)
    elif cli_package:
        if not validate_package(cli_package, repo_root):
            packages = get_packages(repo_root)
            available = ", ".join(sorted(packages.keys())) if packages else "(none)"
            print(colored(f"Error: unknown package '{cli_package}'. Available: {available}", Colors.RED), file=sys.stderr)
            return 1
        package = cli_package
    else:
        # Read task.json.package as inferred source
        task_json_path = target_dir / FILE_TASK_JSON
        task_pkg_value = None
        if task_json_path.is_file():
            task_data = read_json(task_json_path)
            if isinstance(task_data, dict):
                task_pkg_value = task_data.get("package")
        # Only pass string values to resolve_package (guard against malformed JSON)
        task_package = task_pkg_value if isinstance(task_pkg_value, str) else None
        package = resolve_package(task_package=task_package, repo_root=repo_root)

        # Monorepo fallback prohibition
        if package is None:
            packages = get_packages(repo_root)
            available = ", ".join(sorted(packages.keys())) if packages else "(none)"
            print(colored(
                f"Error: monorepo project requires --package (or set default_package in config.yaml). Available: {available}",
                Colors.RED,
            ), file=sys.stderr)
            return 1

    print(colored("=== Initializing Agent Context Files ===", Colors.BLUE))
    print(f"Target dir: {target_dir}")
    print(f"Dev type: {dev_type}")
    if package:
        print(f"Package: {package}")
    print()

    # implement.jsonl
    print(colored("Creating implement.jsonl...", Colors.CYAN))
    implement_entries = get_implement_base()
    if dev_type in ("backend", "test"):
        implement_entries.extend(get_implement_backend(package))
    elif dev_type == "frontend":
        implement_entries.extend(get_implement_frontend(package))
    elif dev_type == "fullstack":
        implement_entries.extend(get_implement_backend(package))
        implement_entries.extend(get_implement_frontend(package))

    implement_file = target_dir / "implement.jsonl"
    _write_jsonl(implement_file, implement_entries)
    print(f"  {colored('✓', Colors.GREEN)} {len(implement_entries)} entries")

    # check.jsonl
    print(colored("Creating check.jsonl...", Colors.CYAN))
    check_entries = get_check_context(repo_root)
    check_file = target_dir / "check.jsonl"
    _write_jsonl(check_file, check_entries)
    print(f"  {colored('✓', Colors.GREEN)} {len(check_entries)} entries")

    # debug.jsonl
    print(colored("Creating debug.jsonl...", Colors.CYAN))
    debug_entries = get_debug_context(repo_root)
    debug_file = target_dir / "debug.jsonl"
    _write_jsonl(debug_file, debug_entries)
    print(f"  {colored('✓', Colors.GREEN)} {len(debug_entries)} entries")

    # Update task.json dev_type and package
    task_json_path = target_dir / FILE_TASK_JSON
    if task_json_path.is_file():
        task_data = read_json(task_json_path)
        if isinstance(task_data, dict):
            task_data["dev_type"] = dev_type
            task_data["package"] = package  # Always sync to match resolved value
            write_json(task_json_path, task_data)

    print()
    print(colored("✓ All context files created", Colors.GREEN))
    print()

    # Show what was auto-injected
    all_injected = [e["file"] for e in implement_entries]
    print(colored("Auto-injected (defaults only):", Colors.YELLOW))
    for f in all_injected:
        print(f"  - {f}")
    print()

    # Scan spec directory for available spec files the AI should consider
    spec_base = repo_root / DIR_WORKFLOW / DIR_SPEC
    if package:
        spec_base = spec_base / package
    available_specs: list[str] = []
    if spec_base.is_dir():
        for md_file in sorted(spec_base.rglob("*.md")):
            rel = str(md_file.relative_to(repo_root))
            if rel not in all_injected:
                available_specs.append(rel)

    if available_specs:
        print(colored("Available spec files (not yet injected):", Colors.BLUE))
        for spec in available_specs:
            print(f"  - {spec}")
        print()

    print(colored("Next steps:", Colors.BLUE))
    print("  1. Review the spec files above and add relevant ones for your task:")
    print(f"     python3 task.py add-context <dir> implement <spec-path> \"<reason>\"")
    print("  2. Set as current: python3 task.py start <dir>")

    return 0


# =============================================================================
# Command: add-context
# =============================================================================

def cmd_add_context(args: argparse.Namespace) -> int:
    """Add entry to JSONL context file."""
    repo_root = get_repo_root()
    target_dir = resolve_task_dir(args.dir, repo_root)

    jsonl_name = args.file
    path = args.path
    reason = args.reason or "Added manually"

    if not target_dir.is_dir():
        print(colored(f"Error: Directory not found: {target_dir}", Colors.RED))
        return 1

    # Support shorthand
    if not jsonl_name.endswith(".jsonl"):
        jsonl_name = f"{jsonl_name}.jsonl"

    jsonl_file = target_dir / jsonl_name
    full_path = repo_root / path

    entry_type = "file"
    if full_path.is_dir():
        entry_type = "directory"
        if not path.endswith("/"):
            path = f"{path}/"
    elif not full_path.is_file():
        print(colored(f"Error: Path not found: {path}", Colors.RED))
        return 1

    # Check if already exists
    if jsonl_file.is_file():
        content = jsonl_file.read_text(encoding="utf-8")
        if f'"{path}"' in content:
            print(colored(f"Warning: Entry already exists for {path}", Colors.YELLOW))
            return 0

    # Add entry
    entry: dict
    if entry_type == "directory":
        entry = {"file": path, "type": "directory", "reason": reason}
    else:
        entry = {"file": path, "reason": reason}

    with jsonl_file.open("a", encoding="utf-8") as f:
        f.write(json.dumps(entry, ensure_ascii=False) + "\n")

    print(colored(f"Added {entry_type}: {path}", Colors.GREEN))
    return 0


# =============================================================================
# Command: validate
# =============================================================================

def cmd_validate(args: argparse.Namespace) -> int:
    """Validate JSONL context files."""
    repo_root = get_repo_root()
    target_dir = resolve_task_dir(args.dir, repo_root)

    if not target_dir.is_dir():
        print(colored("Error: task directory required", Colors.RED))
        return 1

    print(colored("=== Validating Context Files ===", Colors.BLUE))
    print(f"Target dir: {target_dir}")
    print()

    total_errors = 0
    for jsonl_name in ["implement.jsonl", "check.jsonl", "debug.jsonl"]:
        jsonl_file = target_dir / jsonl_name
        errors = _validate_jsonl(jsonl_file, repo_root)
        total_errors += errors

    print()
    if total_errors == 0:
        print(colored("✓ All validations passed", Colors.GREEN))
        return 0
    else:
        print(colored(f"✗ Validation failed ({total_errors} errors)", Colors.RED))
        return 1


def _validate_jsonl(jsonl_file: Path, repo_root: Path) -> int:
    """Validate a single JSONL file."""
    file_name = jsonl_file.name
    errors = 0

    if not jsonl_file.is_file():
        print(f"  {colored(f'{file_name}: not found (skipped)', Colors.YELLOW)}")
        return 0

    line_num = 0
    for line in jsonl_file.read_text(encoding="utf-8").splitlines():
        line_num += 1
        if not line.strip():
            continue

        try:
            data = json.loads(line)
        except json.JSONDecodeError:
            print(f"  {colored(f'{file_name}:{line_num}: Invalid JSON', Colors.RED)}")
            errors += 1
            continue

        file_path = data.get("file")
        entry_type = data.get("type", "file")

        if not file_path:
            print(f"  {colored(f'{file_name}:{line_num}: Missing file field', Colors.RED)}")
            errors += 1
            continue

        full_path = repo_root / file_path
        if entry_type == "directory":
            if not full_path.is_dir():
                print(f"  {colored(f'{file_name}:{line_num}: Directory not found: {file_path}', Colors.RED)}")
                errors += 1
        else:
            if not full_path.is_file():
                print(f"  {colored(f'{file_name}:{line_num}: File not found: {file_path}', Colors.RED)}")
                errors += 1

    if errors == 0:
        print(f"  {colored(f'{file_name}: ✓ ({line_num} entries)', Colors.GREEN)}")
    else:
        print(f"  {colored(f'{file_name}: ✗ ({errors} errors)', Colors.RED)}")

    return errors


# =============================================================================
# Command: list-context
# =============================================================================

def cmd_list_context(args: argparse.Namespace) -> int:
    """List JSONL context entries."""
    repo_root = get_repo_root()
    target_dir = resolve_task_dir(args.dir, repo_root)

    if not target_dir.is_dir():
        print(colored("Error: task directory required", Colors.RED))
        return 1

    print(colored("=== Context Files ===", Colors.BLUE))
    print()

    for jsonl_name in ["implement.jsonl", "check.jsonl", "debug.jsonl"]:
        jsonl_file = target_dir / jsonl_name
        if not jsonl_file.is_file():
            continue

        print(colored(f"[{jsonl_name}]", Colors.CYAN))

        count = 0
        for line in jsonl_file.read_text(encoding="utf-8").splitlines():
            if not line.strip():
                continue

            try:
                data = json.loads(line)
            except json.JSONDecodeError:
                continue

            count += 1
            file_path = data.get("file", "?")
            entry_type = data.get("type", "file")
            reason = data.get("reason", "-")

            if entry_type == "directory":
                print(f"  {colored(f'{count}.', Colors.GREEN)} [DIR] {file_path}")
            else:
                print(f"  {colored(f'{count}.', Colors.GREEN)} {file_path}")
            print(f"     {colored('→', Colors.YELLOW)} {reason}")

        print()

    return 0
