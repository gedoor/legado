"""Bootstrap path setup for multi_agent scripts.

Import this module before importing from common/:

    import _bootstrap  # noqa: F401

This adds the parent scripts/ directory to sys.path so that
`from common.xxx import yyy` works when running scripts directly
via `python3 .trellis/scripts/multi_agent/some_script.py`.
"""

import sys
from pathlib import Path

_scripts_dir = str(Path(__file__).resolve().parent.parent)
if _scripts_dir not in sys.path:
    sys.path.insert(0, _scripts_dir)
