/**
 * Trellis Context Manager
 *
 * Unified context management for OpenCode plugins.
 * Handles detection of oh-my-opencode, .claude/hooks/, and other edge cases.
 *
 * Usage:
 *   import { TrellisContext } from "./trellis-context.js"
 *   const ctx = new TrellisContext(directory)
 *   if (ctx.shouldSkipHook("session-start")) return
 */

import { existsSync, readFileSync, appendFileSync, readdirSync } from "fs"
import { join } from "path"
import { homedir, platform } from "os"
import { execSync } from "child_process"

// Python command: Windows uses 'python', macOS/Linux use 'python3'
const PYTHON_CMD = platform() === "win32" ? "python" : "python3"

// Debug logging
const DEBUG_LOG = "/tmp/trellis-plugin-debug.log"

function debugLog(prefix, ...args) {
  const timestamp = new Date().toISOString()
  const msg = `[${timestamp}] [${prefix}] ${args.map(a => typeof a === "object" ? JSON.stringify(a) : a).join(" ")}\n`
  try {
    appendFileSync(DEBUG_LOG, msg)
  } catch {
    // ignore
  }
}

/**
 * Trellis Context Manager
 *
 * Centralized logic for:
 * - Detecting oh-my-opencode installation
 * - Checking .claude/hooks/ presence
 * - Determining which plugin should handle each hook
 */
export class TrellisContext {
  constructor(directory) {
    this.directory = directory
    this._omoInstalled = null
    this._omoHooksEnabled = null
    this._claudeHooksPresent = {}

    debugLog("context", "TrellisContext initialized", { directory })
  }

  // ============================================================
  // oh-my-opencode Detection
  // ============================================================

  /**
   * Check if oh-my-opencode is installed
   *
   * Detection order:
   * 1. Check if oh-my-opencode.json exists (most reliable)
   * 2. Fallback: check opencode.json plugin list
   */
  isOmoInstalled() {
    if (this._omoInstalled !== null) {
      return this._omoInstalled
    }

    try {
      const configDir = join(homedir(), ".config", "opencode")

      // Method 1: Check oh-my-opencode.json existence (omo-specific config)
      const omoConfigPath = join(configDir, "oh-my-opencode.json")
      if (existsSync(omoConfigPath)) {
        this._omoInstalled = true
        debugLog("context", "omo installed: oh-my-opencode.json exists")
        return true
      }

      // Method 2: Fallback to plugin list check
      const configPath = join(configDir, "opencode.json")
      if (!existsSync(configPath)) {
        this._omoInstalled = false
        debugLog("context", "omo not installed: no config files")
        return false
      }

      const content = readFileSync(configPath, "utf-8")
      const config = JSON.parse(content)
      const plugins = config.plugin || []

      this._omoInstalled = plugins.some(p =>
        typeof p === "string" && p.toLowerCase().includes("oh-my-opencode")
      )

      debugLog("context", "omo installed (plugin list):", this._omoInstalled)
      return this._omoInstalled
    } catch (e) {
      debugLog("context", "omo detection error:", e.message)
      this._omoInstalled = false
      return false
    }
  }

  /**
   * Check if omo's claude_code.hooks is enabled
   * Reads oh-my-opencode.json or defaults to true
   */
  isOmoHooksEnabled() {
    if (this._omoHooksEnabled !== null) {
      return this._omoHooksEnabled
    }

    if (!this.isOmoInstalled()) {
      this._omoHooksEnabled = false
      return false
    }

    try {
      // Check global config
      const globalConfig = join(homedir(), ".config", "opencode", "oh-my-opencode.json")
      if (existsSync(globalConfig)) {
        const content = readFileSync(globalConfig, "utf-8")
        const config = JSON.parse(content)
        if (config.claude_code?.hooks === false) {
          this._omoHooksEnabled = false
          debugLog("context", "omo hooks disabled in global config")
          return false
        }
      }

      // Check project config
      const projectConfig = join(this.directory, "oh-my-opencode.json")
      if (existsSync(projectConfig)) {
        const content = readFileSync(projectConfig, "utf-8")
        const config = JSON.parse(content)
        if (config.claude_code?.hooks === false) {
          this._omoHooksEnabled = false
          debugLog("context", "omo hooks disabled in project config")
          return false
        }
      }

      // Default: enabled
      this._omoHooksEnabled = true
      debugLog("context", "omo hooks enabled (default)")
      return true
    } catch (e) {
      debugLog("context", "omo hooks detection error:", e.message)
      this._omoHooksEnabled = true // Default to enabled
      return true
    }
  }

  // ============================================================
  // .claude/hooks/ Detection
  // ============================================================

  /**
   * Check if a specific .claude/hooks/ file exists
   */
  hasClaudeHook(hookName) {
    if (hookName in this._claudeHooksPresent) {
      return this._claudeHooksPresent[hookName]
    }

    const hookPath = join(this.directory, ".claude", "hooks", `${hookName}.py`)
    const exists = existsSync(hookPath)

    this._claudeHooksPresent[hookName] = exists
    debugLog("context", `claude hook ${hookName}:`, exists)
    return exists
  }

  // ============================================================
  // Trellis Project Detection
  // ============================================================

  /**
   * Check if this is a Trellis-managed project
   */
  isTrellisProject() {
    return existsSync(join(this.directory, ".trellis"))
  }

  /**
   * Get current task directory from .trellis/.current-task
   */
  getCurrentTask() {
    try {
      const currentTaskPath = join(this.directory, ".trellis", ".current-task")
      if (!existsSync(currentTaskPath)) {
        return null
      }
      return readFileSync(currentTaskPath, "utf-8").trim()
    } catch {
      return null
    }
  }

  // ============================================================
  // Hook Decision Logic
  // ============================================================

  /**
   * Determine if our plugin should skip this hook
   * (because omo will handle it via .claude/hooks/)
   *
   * @param {string} hookName - Hook name without extension (e.g., "session-start")
   * @returns {boolean} - true if we should skip, false if we should handle
   */
  shouldSkipHook(hookName) {
    // Not a Trellis project? Skip.
    if (!this.isTrellisProject()) {
      debugLog("context", `shouldSkipHook(${hookName}): skip - not Trellis project`)
      return true
    }

    // omo not installed? We handle it.
    if (!this.isOmoInstalled()) {
      debugLog("context", `shouldSkipHook(${hookName}): handle - omo not installed`)
      return false
    }

    // omo installed but hooks disabled? We handle it.
    if (!this.isOmoHooksEnabled()) {
      debugLog("context", `shouldSkipHook(${hookName}): handle - omo hooks disabled`)
      return false
    }

    // omo installed + hooks enabled + .claude/hooks/ exists? Skip (omo handles).
    if (this.hasClaudeHook(hookName)) {
      debugLog("context", `shouldSkipHook(${hookName}): skip - omo will handle via .claude/hooks/`)
      return true
    }

    // omo installed but no .claude/hooks/ file? We handle it.
    debugLog("context", `shouldSkipHook(${hookName}): handle - no .claude/hooks/ file`)
    return false
  }

  // ============================================================
  // File Reading Utilities
  // ============================================================

  /**
   * Read a file, return null on error
   */
  readFile(filePath) {
    try {
      if (existsSync(filePath)) {
        return readFileSync(filePath, "utf-8")
      }
    } catch {
      // Ignore read errors
    }
    return null
  }

  /**
   * Read a file relative to project directory
   */
  readProjectFile(relativePath) {
    return this.readFile(join(this.directory, relativePath))
  }

  /**
   * Run a Python script and return output
   */
  runScript(scriptPath, cwd = null) {
    try {
      const result = execSync(`${PYTHON_CMD} "${scriptPath}"`, {
        cwd: cwd || this.directory,
        timeout: 10000,
        encoding: "utf-8",
        stdio: ["pipe", "pipe", "pipe"]
      })
      return result || ""
    } catch {
      return ""
    }
  }

  // ============================================================
  // JSONL Reading
  // ============================================================

  /**
   * Read all .md files in a directory
   * @param {string} dirPath - Directory path relative to project root
   * @param {number} maxFiles - Max files to read (prevent huge directories)
   * @returns {Array<{path: string, content: string}>}
   */
  readDirectoryMdFiles(dirPath, maxFiles = 20) {
    const results = []
    const fullPath = join(this.directory, dirPath)

    if (!existsSync(fullPath)) {
      return results
    }

    try {
      const files = readdirSync(fullPath)
        .filter(f => f.endsWith(".md"))
        .sort()
        .slice(0, maxFiles)

      for (const filename of files) {
        const filePath = join(dirPath, filename)
        const content = this.readProjectFile(filePath)
        if (content) {
          results.push({ path: filePath, content })
        }
      }
    } catch {
      // Ignore directory read errors
    }

    return results
  }

  /**
   * Read a JSONL file and load referenced files/directories
   * Supports:
   *   {"file": "path/to/file.md", "reason": "..."}
   *   {"file": "path/to/dir/", "type": "directory", "reason": "..."}
   */
  readJsonlWithFiles(jsonlPath) {
    const results = []
    const content = this.readFile(jsonlPath)
    if (!content) return results

    for (const line of content.split("\n")) {
      if (!line.trim()) continue
      try {
        const item = JSON.parse(line)
        const file = item.file || item.path
        const entryType = item.type || "file"

        if (!file) continue

        if (entryType === "directory") {
          // Read all .md files in directory
          const dirEntries = this.readDirectoryMdFiles(file)
          results.push(...dirEntries)
        } else {
          // Read single file
          const fullPath = join(this.directory, file)
          const fileContent = this.readFile(fullPath)
          if (fileContent) {
            results.push({ path: file, content: fileContent })
          }
        }
      } catch {
        // Ignore parse errors for individual lines
      }
    }
    return results
  }

  /**
   * Build context string from file entries
   */
  buildContextFromEntries(entries) {
    return entries.map(e => `=== ${e.path} ===\n${e.content}`).join("\n\n")
  }
}

// ============================================================
// Context Collector (for synthetic message injection)
// ============================================================

/**
 * Simple context collector for cross-hook communication
 * Similar to oh-my-opencode's contextCollector
 */
class ContextCollector {
  constructor() {
    this.pending = new Map()
    this.processed = new Set()
  }

  /**
   * Store context for a session
   */
  store(sessionID, content) {
    this.pending.set(sessionID, {
      content,
      timestamp: Date.now()
    })
    debugLog("collector", "stored context for session:", sessionID, "length:", content.length)
  }

  /**
   * Check if session has pending context
   */
  hasPending(sessionID) {
    return this.pending.has(sessionID)
  }

  /**
   * Get and consume pending context
   */
  consume(sessionID) {
    const pending = this.pending.get(sessionID)
    this.pending.delete(sessionID)
    return pending
  }

  /**
   * Mark session as processed (for first-message-only injection)
   */
  markProcessed(sessionID) {
    this.processed.add(sessionID)
  }

  /**
   * Check if session was already processed
   */
  isProcessed(sessionID) {
    return this.processed.has(sessionID)
  }

  /**
   * Clear session state
   */
  clear(sessionID) {
    this.pending.delete(sessionID)
    this.processed.delete(sessionID)
  }
}

// Singleton instance
export const contextCollector = new ContextCollector()

// Export debug log for plugins
export { debugLog }
