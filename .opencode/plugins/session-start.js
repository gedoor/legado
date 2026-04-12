/* global process */
/**
 * Trellis Session Start Plugin
 *
 * Injects context when user sends the first message in a session.
 * Uses OpenCode's chat.message + experimental.chat.messages.transform hooks.
 *
 * Compatibility:
 * - If oh-my-opencode handles via .claude/hooks/, this plugin skips
 * - Otherwise, this plugin handles injection
 */

import { existsSync, readFileSync, readdirSync, statSync } from "fs"
import { join } from "path"
import { TrellisContext, contextCollector, debugLog } from "../lib/trellis-context.js"


/**
 * Check current task status and return structured status string.
 * JavaScript equivalent of _get_task_status in Claude's session-start.py.
 */
function getTaskStatus(directory) {
  const trellisDir = join(directory, ".trellis")
  const currentTaskFile = join(trellisDir, ".current-task")

  if (!existsSync(currentTaskFile)) {
    return "Status: NO ACTIVE TASK\nNext: Describe what you want to work on"
  }

  let taskRef
  try {
    taskRef = readFileSync(currentTaskFile, "utf-8").trim()
  } catch {
    return "Status: NO ACTIVE TASK\nNext: Describe what you want to work on"
  }

  if (!taskRef) {
    return "Status: NO ACTIVE TASK\nNext: Describe what you want to work on"
  }

  // Resolve task directory
  let taskDir
  if (taskRef.startsWith("/")) {
    taskDir = taskRef
  } else if (taskRef.startsWith(".trellis/")) {
    taskDir = join(directory, taskRef)
  } else {
    taskDir = join(trellisDir, "tasks", taskRef)
  }

  if (!existsSync(taskDir)) {
    return `Status: STALE POINTER\nTask: ${taskRef}\nNext: Task directory not found. Run: python3 ./.trellis/scripts/task.py finish`
  }

  // Read task.json
  let taskData = {}
  const taskJsonPath = join(taskDir, "task.json")
  if (existsSync(taskJsonPath)) {
    try {
      taskData = JSON.parse(readFileSync(taskJsonPath, "utf-8"))
    } catch {
      // Ignore parse errors
    }
  }

  const taskTitle = taskData.title || taskRef
  const taskStatus = taskData.status || "unknown"

  if (taskStatus === "completed") {
    const dirName = taskDir.split("/").pop()
    return `Status: COMPLETED\nTask: ${taskTitle}\nNext: Archive with \`python3 ./.trellis/scripts/task.py archive ${dirName}\` or start a new task`
  }

  // Check if context is configured (jsonl files exist and non-empty)
  let hasContext = false
  for (const jsonlName of ["implement.jsonl", "check.jsonl", "spec.jsonl"]) {
    const jsonlPath = join(taskDir, jsonlName)
    if (existsSync(jsonlPath)) {
      try {
        const st = statSync(jsonlPath)
        if (st.size > 0) {
          hasContext = true
          break
        }
      } catch {
        // Ignore stat errors
      }
    }
  }

  const hasPrd = existsSync(join(taskDir, "prd.md"))

  if (!hasPrd) {
    return `Status: NOT READY\nTask: ${taskTitle}\nMissing: prd.md not created\nNext: Write PRD, then research → init-context → start`
  }

  if (!hasContext) {
    return `Status: NOT READY\nTask: ${taskTitle}\nMissing: Context not configured (no jsonl files)\nNext: Complete Phase 2 (research → init-context → start) before implementing`
  }

  return `Status: READY\nTask: ${taskTitle}\nNext: Continue with implement or check`
}

/**
 * Build session context for injection
 */
function buildSessionContext(ctx) {
  const directory = ctx.directory
  const trellisDir = join(directory, ".trellis")
  const claudeDir = join(directory, ".claude")
  const opencodeDir = join(directory, ".opencode")

  const parts = []

  // 1. Header
  parts.push(`<trellis-context>
You are starting a new session in a Trellis-managed project.
Read and follow all instructions below carefully.
</trellis-context>`)

  // 2. Current Context (dynamic)
  const contextScript = join(trellisDir, "scripts", "get_context.py")
  if (existsSync(contextScript)) {
    const output = ctx.runScript(contextScript)
    if (output) {
      parts.push("<current-state>")
      parts.push(output)
      parts.push("</current-state>")
    }
  }

  // 3. Workflow Guide
  const workflow = ctx.readProjectFile(".trellis/workflow.md")
  if (workflow) {
    parts.push("<workflow>")
    parts.push(workflow)
    parts.push("</workflow>")
  }

  // 4. Guidelines Index (dynamic discovery, matching Claude's session-start.py)
  parts.push("<guidelines>")
  parts.push("**Note**: The guidelines below are index files — they list available guideline documents and their locations.")
  parts.push("During actual development, you MUST read the specific guideline files listed in each index's Pre-Development Checklist.\n")

  const specDir = join(directory, ".trellis", "spec")
  if (existsSync(specDir)) {
    try {
      const subs = readdirSync(specDir).filter(name => {
        if (name.startsWith(".")) return false
        try {
          return statSync(join(specDir, name)).isDirectory()
        } catch {
          return false
        }
      }).sort()

      for (const sub of subs) {
        const indexFile = join(specDir, sub, "index.md")
        if (existsSync(indexFile)) {
          // Flat spec dir: spec/<layer>/index.md
          const content = ctx.readFile(indexFile)
          if (content) {
            parts.push(`## ${sub}\n${content}\n`)
          }
        } else {
          // Nested package dirs (monorepo): spec/<pkg>/<layer>/index.md
          try {
            const nested = readdirSync(join(specDir, sub)).filter(name => {
              try {
                return statSync(join(specDir, sub, name)).isDirectory()
              } catch {
                return false
              }
            }).sort()

            for (const layer of nested) {
              const nestedIndex = join(specDir, sub, layer, "index.md")
              if (existsSync(nestedIndex)) {
                const content = ctx.readFile(nestedIndex)
                if (content) {
                  parts.push(`## ${sub}/${layer}\n${content}\n`)
                }
              }
            }
          } catch {
            // Ignore directory read errors
          }
        }
      }
    } catch {
      // Ignore spec directory read errors
    }
  }

  parts.push("</guidelines>")

  // 5. Session Instructions - try both .claude and .opencode
  let startMd = ctx.readFile(join(claudeDir, "commands", "trellis", "start.md"))
  if (!startMd) {
    startMd = ctx.readFile(join(opencodeDir, "commands", "trellis", "start.md"))
  }
  if (startMd) {
    parts.push("<instructions>")
    parts.push(startMd)
    parts.push("</instructions>")
  }

  // 6. Task status (R2: check task state for session resume)
  const taskStatus = getTaskStatus(directory)
  parts.push(`<task-status>\n${taskStatus}\n</task-status>`)

  // 7. Final directive (R3: active, not passive)
  parts.push(`<ready>
Context loaded. Steps 1-3 (workflow, context, guidelines) are already injected above — do NOT re-read them.
Start from Step 4. Wait for user's first message, then follow <instructions> to handle their request.
If there is an active task, ask whether to continue it.
</ready>`)

  return parts.join("\n\n")
}

export default async ({ directory }) => {
  const ctx = new TrellisContext(directory)
  debugLog("session", "Plugin loaded, directory:", directory)

  return {
    // chat.message - triggered when user sends a message
    "chat.message": async (input) => {
      try {
        const sessionID = input.sessionID
        const agent = input.agent || "unknown"
        debugLog("session", "chat.message called, sessionID:", sessionID, "agent:", agent)

        // Skip in non-interactive mode
        if (process.env.OPENCODE_NON_INTERACTIVE === "1") {
          debugLog("session", "Skipping - non-interactive mode")
          return
        }

        // Check if we should skip (omo will handle)
        if (ctx.shouldSkipHook("session-start")) {
          debugLog("session", "Skipping - omo will handle via .claude/hooks/")
          return
        }

        // Only inject on first message
        if (contextCollector.isProcessed(sessionID)) {
          debugLog("session", "Skipping - session already processed")
          return
        }

        // Mark session as processed
        contextCollector.markProcessed(sessionID)

        // Build and store context
        const context = buildSessionContext(ctx)
        debugLog("session", "Built context, length:", context.length)

        contextCollector.store(sessionID, context)
        debugLog("session", "Context stored for session:", sessionID)

      } catch (error) {
        debugLog("session", "Error in chat.message:", error.message, error.stack)
      }
    },

    // experimental.chat.messages.transform - modify messages before sending to AI
    "experimental.chat.messages.transform": async (input, output) => {
      try {
        const { messages } = output
        debugLog("session", "messages.transform called, messageCount:", messages?.length)

        if (!messages || messages.length === 0) {
          return
        }

        // Find last user message
        let lastUserMessageIndex = -1
        for (let i = messages.length - 1; i >= 0; i--) {
          if (messages[i].info?.role === "user") {
            lastUserMessageIndex = i
            break
          }
        }

        if (lastUserMessageIndex === -1) {
          debugLog("session", "No user message found")
          return
        }

        const lastUserMessage = messages[lastUserMessageIndex]
        const sessionID = lastUserMessage.info?.sessionID

        debugLog("session", "Found user message, sessionID:", sessionID)

        if (!sessionID || !contextCollector.hasPending(sessionID)) {
          debugLog("session", "No pending context for session")
          return
        }

        // Get and consume pending context
        const pending = contextCollector.consume(sessionID)

        // Find first text part
        const textPartIndex = lastUserMessage.parts?.findIndex(
          p => p.type === "text" && p.text !== undefined
        )

        if (textPartIndex === -1) {
          debugLog("session", "No text part found in user message")
          return
        }

        // Prepend context to the text part (same approach as omo)
        const originalText = lastUserMessage.parts[textPartIndex].text || ""
        lastUserMessage.parts[textPartIndex].text = `${pending.content}\n\n---\n\n${originalText}`

        debugLog("session", "Injected context by prepending to text, length:", pending.content.length)

      } catch (error) {
        debugLog("session", "Error in messages.transform:", error.message, error.stack)
      }
    }
  }
}
