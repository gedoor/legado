#!/usr/bin/env python3
"""
Multi-Agent Pipeline: Process monitoring and log parsing.

Provides:
    tail_follow      - Follow a file like 'tail -f'
    get_last_tool    - Get last tool call from agent log
    get_last_message - Get last assistant text from agent log
    cmd_watch        - Watch agent log in real-time
    cmd_log          - Show recent log entries
"""

from __future__ import annotations

import json
import time
from pathlib import Path

from common.log import Colors

from .status_display import find_agent


# =============================================================================
# Log Parsing
# =============================================================================

def tail_follow(file_path: Path) -> None:
    """Follow a file like 'tail -f', cross-platform compatible."""
    with open(file_path, "r", encoding="utf-8", errors="replace") as f:
        # Seek to end of file
        f.seek(0, 2)

        while True:
            line = f.readline()
            if line:
                print(line, end="", flush=True)
            else:
                time.sleep(0.1)


def get_last_tool(log_file: Path, platform: str = "claude") -> str | None:
    """Get the last tool call from agent log.

    Supports both Claude Code and OpenCode log formats.

    Claude Code format:
        {"type": "assistant", "message": {"content": [{"type": "tool_use", "name": "Read"}]}}

    OpenCode format:
        {"type": "tool_use", "tool": "bash", "state": {"status": "completed"}}
    """
    if not log_file.is_file():
        return None

    try:
        lines = log_file.read_text(encoding="utf-8").splitlines()
        for line in reversed(lines[-100:]):
            try:
                data = json.loads(line)

                if platform == "opencode":
                    # OpenCode format: {"type": "tool_use", "tool": "bash", ...}
                    if data.get("type") == "tool_use":
                        return data.get("tool")
                else:
                    # Claude Code format: {"type": "assistant", "message": {"content": [...]}}
                    if data.get("type") == "assistant":
                        content = data.get("message", {}).get("content", [])
                        for item in content:
                            if item.get("type") == "tool_use":
                                return item.get("name")
            except json.JSONDecodeError:
                continue
    except Exception:
        pass
    return None


def get_last_message(log_file: Path, max_len: int = 100, platform: str = "claude") -> str | None:
    """Get the last assistant text from agent log.

    Supports both Claude Code and OpenCode log formats.

    Claude Code format:
        {"type": "assistant", "message": {"content": [{"type": "text", "text": "..."}]}}

    OpenCode format:
        {"type": "text", "text": "..."}
    """
    if not log_file.is_file():
        return None

    try:
        lines = log_file.read_text(encoding="utf-8").splitlines()
        for line in reversed(lines[-100:]):
            try:
                data = json.loads(line)

                if platform == "opencode":
                    # OpenCode format: {"type": "text", "text": "..."}
                    if data.get("type") == "text":
                        text = data.get("text", "")
                        if text:
                            return text[:max_len]
                else:
                    # Claude Code format: {"type": "assistant", "message": {"content": [...]}}
                    if data.get("type") == "assistant":
                        content = data.get("message", {}).get("content", [])
                        for item in content:
                            if item.get("type") == "text":
                                text = item.get("text", "")
                                if text:
                                    return text[:max_len]
            except json.JSONDecodeError:
                continue
    except Exception:
        pass
    return None


# =============================================================================
# Commands
# =============================================================================

def cmd_watch(target: str, repo_root: Path) -> int:
    """Watch agent log in real-time."""
    agent = find_agent(target, repo_root)
    if not agent:
        print(f"Agent not found: {target}")
        return 1

    worktree = agent.get("worktree_path", "")
    log_file = Path(worktree) / ".agent-log"

    if not log_file.is_file():
        print(f"Log file not found: {log_file}")
        return 1

    print(f"{Colors.BLUE}Watching:{Colors.NC} {log_file}")
    print(f"{Colors.DIM}Press Ctrl+C to stop{Colors.NC}")
    print()

    try:
        tail_follow(log_file)
    except KeyboardInterrupt:
        print()  # Clean newline after Ctrl+C
    return 0


def cmd_log(target: str, repo_root: Path) -> int:
    """Show recent log entries."""
    agent = find_agent(target, repo_root)
    if not agent:
        print(f"Agent not found: {target}")
        return 1

    worktree = agent.get("worktree_path", "")
    platform = agent.get("platform", "claude")
    log_file = Path(worktree) / ".agent-log"

    if not log_file.is_file():
        print(f"Log file not found: {log_file}")
        return 1

    print(f"{Colors.BLUE}=== Recent Log: {target} ==={Colors.NC}")
    print(f"{Colors.DIM}Platform: {platform}{Colors.NC}")
    print()

    lines = log_file.read_text(encoding="utf-8").splitlines()
    for line in lines[-50:]:
        try:
            data = json.loads(line)
            msg_type = data.get("type", "")

            if platform == "opencode":
                # OpenCode format
                if msg_type == "text":
                    text = data.get("text", "")
                    if text:
                        display = text[:300]
                        if len(text) > 300:
                            display += "..."
                        print(f"{Colors.BLUE}[TEXT]{Colors.NC} {display}")
                elif msg_type == "tool_use":
                    tool_name = data.get("tool", "unknown")
                    status = data.get("state", {}).get("status", "")
                    print(f"{Colors.YELLOW}[TOOL]{Colors.NC} {tool_name} ({status})")
                elif msg_type == "step_start":
                    print(f"{Colors.CYAN}[STEP]{Colors.NC} Start")
                elif msg_type == "step_finish":
                    reason = data.get("reason", "")
                    print(f"{Colors.CYAN}[STEP]{Colors.NC} Finish ({reason})")
                elif msg_type == "error":
                    error_msg = data.get("message", "")
                    print(f"{Colors.RED}[ERROR]{Colors.NC} {error_msg}")
            else:
                # Claude Code format
                if msg_type == "system":
                    subtype = data.get("subtype", "")
                    print(f"{Colors.CYAN}[SYSTEM]{Colors.NC} {subtype}")
                elif msg_type == "user":
                    content = data.get("message", {}).get("content", "")
                    if content:
                        print(f"{Colors.GREEN}[USER]{Colors.NC} {content[:200]}")
                elif msg_type == "assistant":
                    content = data.get("message", {}).get("content", [])
                    if content:
                        item = content[0]
                        text = item.get("text")
                        tool = item.get("name")
                        if text:
                            display = text[:300]
                            if len(text) > 300:
                                display += "..."
                            print(f"{Colors.BLUE}[ASSISTANT]{Colors.NC} {display}")
                        elif tool:
                            print(f"{Colors.YELLOW}[TOOL]{Colors.NC} {tool}")
                elif msg_type == "result":
                    tool_name = data.get("tool", "unknown")
                    print(f"{Colors.DIM}[RESULT]{Colors.NC} {tool_name} completed")
        except json.JSONDecodeError:
            continue

    return 0
