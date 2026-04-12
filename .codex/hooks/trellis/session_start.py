#!/usr/bin/env python3
"""Codex session bootstrap hook for Trellis context injection."""

from __future__ import annotations

import sys
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
HOOKS_DIR = SCRIPT_DIR.parent
for candidate in (SCRIPT_DIR, HOOKS_DIR):
    candidate_text = str(candidate)
    if candidate_text not in sys.path:
        sys.path.insert(0, candidate_text)

from common import (
    SESSION_HEADER_MESSAGE,
    SESSION_READY_MESSAGE,
    build_trellis_context,
    configure_stdio,
    emit_json,
    get_project_dir,
    get_session_id,
    is_trellis_project,
    mark_session_seen,
    read_payload,
    should_inject_for_session,
)


def main() -> None:
    configure_stdio()
    payload = read_payload()
    project_dir = get_project_dir(payload)

    if not is_trellis_project(project_dir):
        emit_json()
        return

    session_id = get_session_id(payload)
    if not should_inject_for_session(project_dir, session_id):
        emit_json()
        return

    context = build_trellis_context(
        project_dir,
        header_tag="session-context",
        header_message=SESSION_HEADER_MESSAGE,
        ready_message=SESSION_READY_MESSAGE,
    )
    emit_json({"additionalContext": context})
    mark_session_seen(project_dir, session_id)


if __name__ == "__main__":
    main()
