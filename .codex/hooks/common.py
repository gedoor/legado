#!/usr/bin/env python3
"""Shared helpers for Trellis Codex hooks."""

from __future__ import annotations

import io
import json
import subprocess
import sys
from pathlib import Path
from typing import Any

SESSION_HEADER_MESSAGE = (
    "You are starting a new session in a Trellis-managed project.\n"
    "Read and follow all instructions below carefully."
)
SESSION_READY_MESSAGE = (
    "Context loaded. Wait for user's first message, then follow "
    "<instructions> to handle their request."
)
SESSION_STATE_PATH = Path(".codex") / "hooks" / ".session-state.json"
SESSION_STATE_VERSION = 1


def _configure_stream(stream: Any) -> Any:
    if hasattr(stream, "reconfigure"):
        stream.reconfigure(encoding="utf-8", errors="replace")
        return stream
    if hasattr(stream, "detach"):
        return io.TextIOWrapper(
            stream.detach(),
            encoding="utf-8",
            errors="replace",
        )
    return stream


def configure_stdio() -> None:
    """Force UTF-8 stdio on Windows so hook I/O is stable."""
    if sys.platform != "win32":
        return

    sys.stdout = _configure_stream(sys.stdout)
    sys.stderr = _configure_stream(sys.stderr)
    sys.stdin = _configure_stream(sys.stdin)


def read_payload() -> dict[str, Any]:
    """Read JSON payload from stdin, tolerating Codex nested input wrappers."""
    if sys.stdin.isatty():
        return {}

    raw = sys.stdin.read().lstrip("\ufeff").strip()
    if not raw:
        return {}

    try:
        data = json.loads(raw)
    except json.JSONDecodeError:
        return {}

    if not isinstance(data, dict):
        return {}

    nested_input = data.get("input")
    if isinstance(nested_input, dict):
        return nested_input

    return data


def emit_json(payload: dict[str, Any] | None = None) -> None:
    print(json.dumps(payload or {}, ensure_ascii=False), flush=True)


def get_session_id(payload: dict[str, Any]) -> str | None:
    session_id = payload.get("session_id")
    if not isinstance(session_id, str):
        return None

    normalized = session_id.strip()
    return normalized or None


def read_file(path: Path, fallback: str = "") -> str:
    try:
        return path.read_text(encoding="utf-8", errors="replace")
    except OSError:
        return fallback


def get_session_state_path(project_dir: Path) -> Path:
    return project_dir / SESSION_STATE_PATH


def load_seen_session_ids(project_dir: Path) -> set[str]:
    state_path = get_session_state_path(project_dir)
    try:
        raw = json.loads(state_path.read_text(encoding="utf-8", errors="replace"))
    except (OSError, json.JSONDecodeError):
        return set()

    if not isinstance(raw, dict):
        return set()

    seen_session_ids = raw.get("seen_session_ids")
    if not isinstance(seen_session_ids, list):
        return set()

    return {
        session_id.strip()
        for session_id in seen_session_ids
        if isinstance(session_id, str) and session_id.strip()
    }


def mark_session_seen(project_dir: Path, session_id: str | None) -> None:
    if session_id is None:
        return

    state_path = get_session_state_path(project_dir)
    seen_session_ids = load_seen_session_ids(project_dir)
    seen_session_ids.add(session_id)
    payload = {
        "version": SESSION_STATE_VERSION,
        "seen_session_ids": sorted(seen_session_ids),
    }
    temp_path = state_path.with_name(f"{state_path.name}.tmp")

    try:
        state_path.parent.mkdir(parents=True, exist_ok=True)
        temp_path.write_text(
            json.dumps(payload, indent=2, ensure_ascii=False),
            encoding="utf-8",
        )
        temp_path.replace(state_path)
    except OSError:
        try:
            temp_path.unlink()
        except OSError:
            pass


def should_inject_for_session(project_dir: Path, session_id: str | None) -> bool:
    if session_id is None:
        return True

    seen_session_ids = load_seen_session_ids(project_dir)
    return session_id not in seen_session_ids


def strip_front_matter(content: str) -> str:
    if not content.startswith("---"):
        return content

    lines = content.splitlines()
    for index in range(1, len(lines)):
        if lines[index].strip() == "---":
            return "\n".join(lines[index + 1 :]).lstrip()
    return content


def run_python_script(script_path: Path, cwd: Path, args: list[str] | None = None) -> str:
    """Run a Python script with the current interpreter and return stdout."""
    if not script_path.exists():
        return ""

    command = [sys.executable, "-W", "ignore", str(script_path)]
    if args:
        command.extend(args)

    try:
        result = subprocess.run(
            command,
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
            timeout=10,
            cwd=str(cwd),
        )
    except (subprocess.TimeoutExpired, OSError):
        return ""

    if result.returncode != 0:
        return ""
    return result.stdout.strip()


def get_project_dir(payload: dict[str, Any]) -> Path:
    cwd = payload.get("cwd")
    if isinstance(cwd, str) and cwd.strip():
        return Path(cwd).resolve()
    return Path.cwd().resolve()


def is_trellis_project(project_dir: Path) -> bool:
    return (project_dir / ".trellis").is_dir()


def get_start_instructions(project_dir: Path) -> str:
    skill_path = project_dir / ".agents" / "skills" / "start" / "SKILL.md"
    return strip_front_matter(read_file(skill_path))


def build_trellis_context(
    project_dir: Path,
    *,
    header_tag: str,
    header_message: str,
    ready_message: str,
) -> str:
    trellis_dir = project_dir / ".trellis"
    context_output = run_python_script(trellis_dir / "scripts" / "get_context.py", project_dir)
    packages_output = run_python_script(
        trellis_dir / "scripts" / "get_context.py",
        project_dir,
        args=["--mode", "packages"],
    )
    workflow = read_file(trellis_dir / "workflow.md", "No workflow.md found")
    guides = read_file(
        trellis_dir / "spec" / "guides" / "index.md",
        "Not configured",
    )
    instructions = get_start_instructions(project_dir)

    parts = [
        f"<{header_tag}>",
        header_message,
        f"</{header_tag}>",
        "",
        "<current-state>",
        context_output or "No context available",
        "</current-state>",
        "",
        "<workflow>",
        workflow,
        "</workflow>",
        "",
        "<guidelines>",
        "## Packages",
        packages_output or "Not configured",
        "",
        "## Guides",
        guides,
        "</guidelines>",
    ]

    if instructions:
        parts.extend(
            [
                "",
                "<instructions>",
                instructions,
                "</instructions>",
            ]
        )

    parts.extend(
        [
            "",
            "<ready>",
            ready_message,
            "</ready>",
        ]
    )

    return "\n".join(parts)
