#!/usr/bin/env python3
"""
Task CRUD operations.

Provides:
    ensure_tasks_dir   - Ensure tasks directory exists
    cmd_create         - Create a new task
    cmd_archive        - Archive completed task
    cmd_set_branch     - Set git branch for task
    cmd_set_base_branch - Set PR target branch
    cmd_set_scope      - Set scope for PR title
    cmd_add_subtask    - Link child task to parent
    cmd_remove_subtask - Unlink child task from parent
"""

from __future__ import annotations

import argparse
import re
import sys
from datetime import datetime
from pathlib import Path

from .config import (
    get_packages,
    is_monorepo,
    resolve_package,
    validate_package,
)
from .git import run_git
from .io import read_json, write_json
from .log import Colors, colored
from .paths import (
    DIR_ARCHIVE,
    DIR_TASKS,
    DIR_WORKFLOW,
    FILE_TASK_JSON,
    clear_current_task,
    generate_task_date_prefix,
    get_current_task,
    get_developer,
    get_repo_root,
    get_tasks_dir,
)
from .task_utils import (
    archive_task_complete,
    find_task_by_name,
    resolve_task_dir,
    run_task_hooks,
)


# =============================================================================
# Helper Functions
# =============================================================================

def _slugify(title: str) -> str:
    """Convert title to slug (only works with ASCII)."""
    result = title.lower()
    result = re.sub(r"[^a-z0-9]", "-", result)
    result = re.sub(r"-+", "-", result)
    result = result.strip("-")
    return result


def ensure_tasks_dir(repo_root: Path) -> Path:
    """Ensure tasks directory exists."""
    tasks_dir = get_tasks_dir(repo_root)
    archive_dir = tasks_dir / "archive"

    if not tasks_dir.exists():
        tasks_dir.mkdir(parents=True)
        print(colored(f"Created tasks directory: {tasks_dir}", Colors.GREEN), file=sys.stderr)

    if not archive_dir.exists():
        archive_dir.mkdir(parents=True)

    return tasks_dir


# =============================================================================
# Command: create
# =============================================================================

def cmd_create(args: argparse.Namespace) -> int:
    """Create a new task."""
    repo_root = get_repo_root()

    if not args.title:
        print(colored("Error: title is required", Colors.RED), file=sys.stderr)
        return 1

    # Validate --package (CLI source: fail-fast)
    package: str | None = getattr(args, "package", None)
    if not is_monorepo(repo_root):
        # Single-repo: ignore --package, no package prefix
        if package:
            print(colored(f"Warning: --package ignored in single-repo project", Colors.YELLOW), file=sys.stderr)
        package = None
    elif package:
        if not validate_package(package, repo_root):
            packages = get_packages(repo_root)
            available = ", ".join(sorted(packages.keys())) if packages else "(none)"
            print(colored(f"Error: unknown package '{package}'. Available: {available}", Colors.RED), file=sys.stderr)
            return 1
    else:
        # Inferred: default_package → None (no task.json yet for create)
        package = resolve_package(repo_root=repo_root)

    # Default assignee to current developer
    assignee = args.assignee
    if not assignee:
        assignee = get_developer(repo_root)
        if not assignee:
            print(colored("Error: No developer set. Run init_developer.py first or use --assignee", Colors.RED), file=sys.stderr)
            return 1

    ensure_tasks_dir(repo_root)

    # Get current developer as creator
    creator = get_developer(repo_root) or assignee

    # Generate slug if not provided
    slug = args.slug or _slugify(args.title)
    if not slug:
        print(colored("Error: could not generate slug from title", Colors.RED), file=sys.stderr)
        return 1

    # Create task directory with MM-DD-slug format
    tasks_dir = get_tasks_dir(repo_root)
    date_prefix = generate_task_date_prefix()
    dir_name = f"{date_prefix}-{slug}"
    task_dir = tasks_dir / dir_name
    task_json_path = task_dir / FILE_TASK_JSON

    if task_dir.exists():
        print(colored(f"Warning: Task directory already exists: {dir_name}", Colors.YELLOW), file=sys.stderr)
    else:
        task_dir.mkdir(parents=True)

    today = datetime.now().strftime("%Y-%m-%d")

    # Record current branch as base_branch (PR target)
    _, branch_out, _ = run_git(["branch", "--show-current"], cwd=repo_root)
    current_branch = branch_out.strip() or "main"

    task_data = {
        "id": slug,
        "name": slug,
        "title": args.title,
        "description": args.description or "",
        "status": "planning",
        "dev_type": None,
        "scope": None,
        "package": package,
        "priority": args.priority,
        "creator": creator,
        "assignee": assignee,
        "createdAt": today,
        "completedAt": None,
        "branch": None,
        "base_branch": current_branch,
        "worktree_path": None,
        "current_phase": 0,
        "next_action": [
            {"phase": 1, "action": "implement"},
            {"phase": 2, "action": "check"},
            {"phase": 3, "action": "finish"},
            {"phase": 4, "action": "create-pr"},
        ],
        "commit": None,
        "pr_url": None,
        "subtasks": [],
        "children": [],
        "parent": None,
        "relatedFiles": [],
        "notes": "",
        "meta": {},
    }

    write_json(task_json_path, task_data)

    # Handle --parent: establish bidirectional link
    if args.parent:
        parent_dir = resolve_task_dir(args.parent, repo_root)
        parent_json_path = parent_dir / FILE_TASK_JSON
        if not parent_json_path.is_file():
            print(colored(f"Warning: Parent task.json not found: {args.parent}", Colors.YELLOW), file=sys.stderr)
        else:
            parent_data = read_json(parent_json_path)
            if parent_data:
                # Add child to parent's children list
                parent_children = parent_data.get("children", [])
                if dir_name not in parent_children:
                    parent_children.append(dir_name)
                    parent_data["children"] = parent_children
                    write_json(parent_json_path, parent_data)

                # Set parent in child's task.json
                task_data["parent"] = parent_dir.name
                write_json(task_json_path, task_data)

                print(colored(f"Linked as child of: {parent_dir.name}", Colors.GREEN), file=sys.stderr)

    print(colored(f"Created task: {dir_name}", Colors.GREEN), file=sys.stderr)
    print("", file=sys.stderr)
    print(colored("Next steps:", Colors.BLUE), file=sys.stderr)
    print("  1. Create prd.md with requirements", file=sys.stderr)
    print("  2. Run: python3 task.py init-context <dir> <dev_type>", file=sys.stderr)
    print("  3. Run: python3 task.py start <dir>", file=sys.stderr)
    print("", file=sys.stderr)

    # Output relative path for script chaining
    print(f"{DIR_WORKFLOW}/{DIR_TASKS}/{dir_name}")

    run_task_hooks("after_create", task_json_path, repo_root)
    return 0


# =============================================================================
# Command: archive
# =============================================================================

def cmd_archive(args: argparse.Namespace) -> int:
    """Archive completed task."""
    repo_root = get_repo_root()
    task_name = args.name

    if not task_name:
        print(colored("Error: Task name is required", Colors.RED), file=sys.stderr)
        return 1

    tasks_dir = get_tasks_dir(repo_root)

    # Find task directory
    task_dir = find_task_by_name(task_name, tasks_dir)

    if not task_dir or not task_dir.is_dir():
        print(colored(f"Error: Task not found: {task_name}", Colors.RED), file=sys.stderr)
        print("Active tasks:", file=sys.stderr)
        # Import lazily to avoid circular dependency
        from .tasks import iter_active_tasks
        for t in iter_active_tasks(tasks_dir):
            print(f"  - {t.dir_name}/", file=sys.stderr)
        return 1

    dir_name = task_dir.name
    task_json_path = task_dir / FILE_TASK_JSON

    # Update status before archiving
    today = datetime.now().strftime("%Y-%m-%d")
    if task_json_path.is_file():
        data = read_json(task_json_path)
        if data:
            data["status"] = "completed"
            data["completedAt"] = today
            write_json(task_json_path, data)

            # Handle subtask relationships on archive
            task_parent = data.get("parent")
            task_children = data.get("children", [])

            # If this is a child, remove from parent's children list
            if task_parent:
                parent_dir = find_task_by_name(task_parent, tasks_dir)
                if parent_dir:
                    parent_json = parent_dir / FILE_TASK_JSON
                    if parent_json.is_file():
                        parent_data = read_json(parent_json)
                        if parent_data:
                            parent_children = parent_data.get("children", [])
                            if dir_name in parent_children:
                                parent_children.remove(dir_name)
                                parent_data["children"] = parent_children
                                write_json(parent_json, parent_data)

            # If this is a parent, clear parent field in all children
            if task_children:
                for child_name in task_children:
                    child_dir_path = find_task_by_name(child_name, tasks_dir)
                    if child_dir_path:
                        child_json = child_dir_path / FILE_TASK_JSON
                        if child_json.is_file():
                            child_data = read_json(child_json)
                            if child_data:
                                child_data["parent"] = None
                                write_json(child_json, child_data)

    # Clear if current task
    current = get_current_task(repo_root)
    if current and dir_name in current:
        clear_current_task(repo_root)

    # Archive
    result = archive_task_complete(task_dir, repo_root)
    if "archived_to" in result:
        archive_dest = Path(result["archived_to"])
        year_month = archive_dest.parent.name
        print(colored(f"Archived: {dir_name} -> archive/{year_month}/", Colors.GREEN), file=sys.stderr)

        # Auto-commit unless --no-commit
        if not getattr(args, "no_commit", False):
            _auto_commit_archive(dir_name, repo_root)

        # Return the archive path
        print(f"{DIR_WORKFLOW}/{DIR_TASKS}/{DIR_ARCHIVE}/{year_month}/{dir_name}")

        # Run hooks with the archived path
        archived_json = archive_dest / FILE_TASK_JSON
        run_task_hooks("after_archive", archived_json, repo_root)
        return 0

    return 1


def _auto_commit_archive(task_name: str, repo_root: Path) -> None:
    """Stage .trellis/tasks/ changes and commit after archive."""
    tasks_rel = f"{DIR_WORKFLOW}/{DIR_TASKS}"
    run_git(["add", "-A", tasks_rel], cwd=repo_root)

    # Check if there are staged changes
    rc, _, _ = run_git(
        ["diff", "--cached", "--quiet", "--", tasks_rel], cwd=repo_root
    )
    if rc == 0:
        print("[OK] No task changes to commit.", file=sys.stderr)
        return

    commit_msg = f"chore(task): archive {task_name}"
    rc, _, err = run_git(["commit", "-m", commit_msg], cwd=repo_root)
    if rc == 0:
        print(f"[OK] Auto-committed: {commit_msg}", file=sys.stderr)
    else:
        print(f"[WARN] Auto-commit failed: {err.strip()}", file=sys.stderr)


# =============================================================================
# Command: add-subtask
# =============================================================================

def cmd_add_subtask(args: argparse.Namespace) -> int:
    """Link a child task to a parent task."""
    repo_root = get_repo_root()

    parent_dir = resolve_task_dir(args.parent_dir, repo_root)
    child_dir = resolve_task_dir(args.child_dir, repo_root)

    parent_json_path = parent_dir / FILE_TASK_JSON
    child_json_path = child_dir / FILE_TASK_JSON

    if not parent_json_path.is_file():
        print(colored(f"Error: Parent task.json not found: {args.parent_dir}", Colors.RED), file=sys.stderr)
        return 1

    if not child_json_path.is_file():
        print(colored(f"Error: Child task.json not found: {args.child_dir}", Colors.RED), file=sys.stderr)
        return 1

    parent_data = read_json(parent_json_path)
    child_data = read_json(child_json_path)

    if not parent_data or not child_data:
        print(colored("Error: Failed to read task.json", Colors.RED), file=sys.stderr)
        return 1

    # Check if child already has a parent
    existing_parent = child_data.get("parent")
    if existing_parent:
        print(colored(f"Error: Child task already has a parent: {existing_parent}", Colors.RED), file=sys.stderr)
        return 1

    # Add child to parent's children list
    parent_children = parent_data.get("children", [])
    child_dir_name = child_dir.name
    if child_dir_name not in parent_children:
        parent_children.append(child_dir_name)
        parent_data["children"] = parent_children

    # Set parent in child's task.json
    child_data["parent"] = parent_dir.name

    # Write both
    write_json(parent_json_path, parent_data)
    write_json(child_json_path, child_data)

    print(colored(f"Linked: {child_dir.name} -> {parent_dir.name}", Colors.GREEN), file=sys.stderr)
    return 0


# =============================================================================
# Command: remove-subtask
# =============================================================================

def cmd_remove_subtask(args: argparse.Namespace) -> int:
    """Unlink a child task from a parent task."""
    repo_root = get_repo_root()

    parent_dir = resolve_task_dir(args.parent_dir, repo_root)
    child_dir = resolve_task_dir(args.child_dir, repo_root)

    parent_json_path = parent_dir / FILE_TASK_JSON
    child_json_path = child_dir / FILE_TASK_JSON

    if not parent_json_path.is_file():
        print(colored(f"Error: Parent task.json not found: {args.parent_dir}", Colors.RED), file=sys.stderr)
        return 1

    if not child_json_path.is_file():
        print(colored(f"Error: Child task.json not found: {args.child_dir}", Colors.RED), file=sys.stderr)
        return 1

    parent_data = read_json(parent_json_path)
    child_data = read_json(child_json_path)

    if not parent_data or not child_data:
        print(colored("Error: Failed to read task.json", Colors.RED), file=sys.stderr)
        return 1

    # Remove child from parent's children list
    parent_children = parent_data.get("children", [])
    child_dir_name = child_dir.name
    if child_dir_name in parent_children:
        parent_children.remove(child_dir_name)
        parent_data["children"] = parent_children

    # Clear parent in child's task.json
    child_data["parent"] = None

    # Write both
    write_json(parent_json_path, parent_data)
    write_json(child_json_path, child_data)

    print(colored(f"Unlinked: {child_dir.name} from {parent_dir.name}", Colors.GREEN), file=sys.stderr)
    return 0


# =============================================================================
# Command: set-branch
# =============================================================================

def cmd_set_branch(args: argparse.Namespace) -> int:
    """Set git branch for task."""
    repo_root = get_repo_root()
    target_dir = resolve_task_dir(args.dir, repo_root)
    branch = args.branch

    if not branch:
        print(colored("Error: Missing arguments", Colors.RED))
        print("Usage: python3 task.py set-branch <task-dir> <branch-name>")
        return 1

    task_json = target_dir / FILE_TASK_JSON
    if not task_json.is_file():
        print(colored(f"Error: task.json not found at {target_dir}", Colors.RED))
        return 1

    data = read_json(task_json)
    if not data:
        return 1

    data["branch"] = branch
    write_json(task_json, data)

    print(colored(f"✓ Branch set to: {branch}", Colors.GREEN))
    print()
    print(colored("Now you can start the multi-agent pipeline:", Colors.BLUE))
    print(f"  python3 ./.trellis/scripts/multi_agent/start.py {args.dir}")
    return 0


# =============================================================================
# Command: set-base-branch
# =============================================================================

def cmd_set_base_branch(args: argparse.Namespace) -> int:
    """Set the base branch (PR target) for task."""
    repo_root = get_repo_root()
    target_dir = resolve_task_dir(args.dir, repo_root)
    base_branch = args.base_branch

    if not base_branch:
        print(colored("Error: Missing arguments", Colors.RED))
        print("Usage: python3 task.py set-base-branch <task-dir> <base-branch>")
        print("Example: python3 task.py set-base-branch <dir> develop")
        print()
        print("This sets the target branch for PR (the branch your feature will merge into).")
        return 1

    task_json = target_dir / FILE_TASK_JSON
    if not task_json.is_file():
        print(colored(f"Error: task.json not found at {target_dir}", Colors.RED))
        return 1

    data = read_json(task_json)
    if not data:
        return 1

    data["base_branch"] = base_branch
    write_json(task_json, data)

    print(colored(f"✓ Base branch set to: {base_branch}", Colors.GREEN))
    print(f"  PR will target: {base_branch}")
    return 0


# =============================================================================
# Command: set-scope
# =============================================================================

def cmd_set_scope(args: argparse.Namespace) -> int:
    """Set scope for PR title."""
    repo_root = get_repo_root()
    target_dir = resolve_task_dir(args.dir, repo_root)
    scope = args.scope

    if not scope:
        print(colored("Error: Missing arguments", Colors.RED))
        print("Usage: python3 task.py set-scope <task-dir> <scope>")
        return 1

    task_json = target_dir / FILE_TASK_JSON
    if not task_json.is_file():
        print(colored(f"Error: task.json not found at {target_dir}", Colors.RED))
        return 1

    data = read_json(task_json)
    if not data:
        return 1

    data["scope"] = scope
    write_json(task_json, data)

    print(colored(f"✓ Scope set to: {scope}", Colors.GREEN))
    return 0
