/**
 * Trellis Context Injection Plugin
 *
 * Injects context when Task tool is called with supported subagent types.
 * Uses OpenCode's tool.execute.before hook.
 *
 * Compatibility:
 * - If oh-my-opencode handles via .claude/hooks/, this plugin skips
 * - Otherwise, this plugin handles injection
 */

import { existsSync, writeFileSync } from "fs"
import { join } from "path"
import { TrellisContext, debugLog } from "../lib/trellis-context.js"

// Supported subagent types
const AGENTS_ALL = ["implement", "check", "debug", "research"]
const AGENTS_REQUIRE_TASK = ["implement", "check", "debug"]
// Agents that don't update phase (can be called at any time)
const AGENTS_NO_PHASE_UPDATE = ["debug", "research"]

/**
 * Update current_phase in task.json based on subagent_type
 */
function updateCurrentPhase(ctx, taskDir, subagentType) {
  if (AGENTS_NO_PHASE_UPDATE.includes(subagentType)) {
    return
  }

  const taskJsonPath = join(ctx.directory, taskDir, "task.json")
  const content = ctx.readFile(taskJsonPath)
  if (!content) return

  try {
    const taskData = JSON.parse(content)
    const currentPhase = taskData.current_phase || 0
    const nextActions = taskData.next_action || []

    // Map action names to subagent types
    const actionToAgent = {
      "implement": "implement",
      "check": "check",
      "finish": "check"  // finish uses check agent
    }

    // Find the next phase that matches this subagent_type
    let newPhase = null
    for (const action of nextActions) {
      const phaseNum = action.phase || 0
      const actionName = action.action || ""
      const expectedAgent = actionToAgent[actionName]

      // Only consider phases after current_phase
      if (phaseNum > currentPhase && expectedAgent === subagentType) {
        newPhase = phaseNum
        break
      }
    }

    if (newPhase !== null) {
      taskData.current_phase = newPhase
      writeFileSync(taskJsonPath, JSON.stringify(taskData, null, 2))
      debugLog("inject", "Updated current_phase to:", newPhase)
    }
  } catch (e) {
    debugLog("inject", "Error updating phase:", e.message)
  }
}

/**
 * Get context for implement agent
 */
function getImplementContext(ctx, taskDir) {
  const parts = []

  // 1. Read implement.jsonl (or fallback to spec.jsonl)
  let jsonlPath = join(ctx.directory, taskDir, "implement.jsonl")
  let entries = ctx.readJsonlWithFiles(jsonlPath)

  if (entries.length === 0) {
    // Fallback to spec.jsonl
    jsonlPath = join(ctx.directory, taskDir, "spec.jsonl")
    entries = ctx.readJsonlWithFiles(jsonlPath)
  }

  if (entries.length > 0) {
    parts.push(ctx.buildContextFromEntries(entries))
  }

  // 2. Requirements document
  const prd = ctx.readProjectFile(join(taskDir, "prd.md"))
  if (prd) {
    parts.push(`=== ${taskDir}/prd.md (Requirements) ===\n${prd}`)
  }

  // 3. Technical design
  const info = ctx.readProjectFile(join(taskDir, "info.md"))
  if (info) {
    parts.push(`=== ${taskDir}/info.md (Technical Design) ===\n${info}`)
  }

  return parts.join("\n\n")
}

/**
 * Get context for check agent
 */
function getCheckContext(ctx, taskDir) {
  const parts = []

  // 1. Read check.jsonl
  const jsonlPath = join(ctx.directory, taskDir, "check.jsonl")
  const entries = ctx.readJsonlWithFiles(jsonlPath)

  if (entries.length > 0) {
    parts.push(ctx.buildContextFromEntries(entries))
  } else {
    // Fallback: hardcoded check files + spec.jsonl
    const checkFiles = [
      [".opencode/commands/trellis/finish-work.md", "Finish work checklist"],
      [".opencode/commands/trellis/check-cross-layer.md", "Cross-layer check spec"],
      [".opencode/commands/trellis/check-backend.md", "Backend check spec"],
      [".opencode/commands/trellis/check-frontend.md", "Frontend check spec"],
    ]
    for (const [f, description] of checkFiles) {
      const content = ctx.readProjectFile(f)
      if (content) {
        parts.push(`=== ${f} (${description}) ===\n${content}`)
      }
    }

    // Add spec.jsonl
    const specJsonlPath = join(ctx.directory, taskDir, "spec.jsonl")
    const specEntries = ctx.readJsonlWithFiles(specJsonlPath)
    for (const entry of specEntries) {
      parts.push(`=== ${entry.path} (Dev spec) ===\n${entry.content}`)
    }
  }

  // 2. Requirements document
  const prd = ctx.readProjectFile(join(taskDir, "prd.md"))
  if (prd) {
    parts.push(`=== ${taskDir}/prd.md (Requirements - for understanding intent) ===\n${prd}`)
  }

  return parts.join("\n\n")
}

/**
 * Get context for finish phase (final check before PR)
 */
function getFinishContext(ctx, taskDir) {
  const parts = []

  // 1. Try finish.jsonl first
  const jsonlPath = join(ctx.directory, taskDir, "finish.jsonl")
  const entries = ctx.readJsonlWithFiles(jsonlPath)

  if (entries.length > 0) {
    parts.push(ctx.buildContextFromEntries(entries))
  } else {
    // Fallback: only finish-work.md (lightweight)
    const finishWork = ctx.readProjectFile(".opencode/commands/trellis/finish-work.md")
    if (finishWork) {
      parts.push(`=== .opencode/commands/trellis/finish-work.md (Finish checklist) ===\n${finishWork}`)
    }
  }

  // 2. Spec update process (for active spec sync)
  const updateSpec = ctx.readProjectFile(".opencode/commands/trellis/update-spec.md")
  if (updateSpec) {
    parts.push(`=== .opencode/commands/trellis/update-spec.md (Spec update process) ===\n${updateSpec}`)
  }

  // 3. Requirements document (for verifying requirements are met)
  const prd = ctx.readProjectFile(join(taskDir, "prd.md"))
  if (prd) {
    parts.push(`=== ${taskDir}/prd.md (Requirements - verify all met) ===\n${prd}`)
  }

  return parts.join("\n\n")
}

/**
 * Get context for debug agent
 */
function getDebugContext(ctx, taskDir) {
  const parts = []

  // 1. Read debug.jsonl (or fallback to spec.jsonl + check files)
  const jsonlPath = join(ctx.directory, taskDir, "debug.jsonl")
  const entries = ctx.readJsonlWithFiles(jsonlPath)

  if (entries.length > 0) {
    parts.push(ctx.buildContextFromEntries(entries))
  } else {
    // Fallback: use spec.jsonl + hardcoded check files
    const specJsonlPath = join(ctx.directory, taskDir, "spec.jsonl")
    const specEntries = ctx.readJsonlWithFiles(specJsonlPath)
    for (const entry of specEntries) {
      parts.push(`=== ${entry.path} (Dev spec) ===\n${entry.content}`)
    }

    const checkFiles = [
      [".opencode/commands/trellis/check-backend.md", "Backend check spec"],
      [".opencode/commands/trellis/check-frontend.md", "Frontend check spec"],
      [".opencode/commands/trellis/check-cross-layer.md", "Cross-layer check spec"],
    ]
    for (const [f, description] of checkFiles) {
      const content = ctx.readProjectFile(f)
      if (content) {
        parts.push(`=== ${f} (${description}) ===\n${content}`)
      }
    }
  }

  // 2. Codex review output (if exists)
  const codex = ctx.readProjectFile(join(taskDir, "codex-review-output.txt"))
  if (codex) {
    parts.push(`=== ${taskDir}/codex-review-output.txt (Codex Review Results) ===\n${codex}`)
  }

  return parts.join("\n\n")
}

/**
 * Get context for research agent
 */
function getResearchContext(ctx, taskDir) {
  const parts = []

  parts.push(`## Project Spec Directory Structure

\`\`\`
.trellis/spec/
├── shared/      # Cross-project common specs
├── frontend/    # Frontend standards
├── backend/     # Backend standards
└── guides/      # Thinking guides

.trellis/big-question/  # Known issues and pitfalls
\`\`\`

## Search Tips

- Spec files: \`.trellis/spec/**/*.md\`
- Known issues: \`.trellis/big-question/\`
- Code search: Use Glob and Grep tools
- Tech solutions: Use mcp__exa__web_search_exa or mcp__exa__get_code_context_exa`)

  if (taskDir) {
    const jsonlPath = join(ctx.directory, taskDir, "research.jsonl")
    const entries = ctx.readJsonlWithFiles(jsonlPath)
    if (entries.length > 0) {
      parts.push("\n## Additional Search Context\n")
      parts.push(ctx.buildContextFromEntries(entries))
    }
  }

  return parts.join("\n\n")
}

/**
 * Build enhanced prompt with context
 */
function buildPrompt(agentType, originalPrompt, context, isFinish = false) {
  const templates = {
    implement: `# Implement Agent Task

You are the Implement Agent in the Multi-Agent Pipeline.

## Your Context

${context}

---

## Your Task

${originalPrompt}

---

## Workflow

1. **Understand specs** - All dev specs are injected above
2. **Understand requirements** - Read requirements and technical design
3. **Implement feature** - Follow specs and design
4. **Self-check** - Ensure code quality

## Important Constraints

- Do NOT execute git commit unless the user explicitly asks for it
- Follow all dev specs injected above
- Report list of modified/created files when done`,

    check: isFinish ? `# Finish Agent Task

You are performing the final check before creating a PR.

## Your Context

${context}

---

## Your Task

${originalPrompt}

---

## Workflow

1. **Review changes** - Run \`git diff --name-only\` to see all changed files
2. **Verify requirements** - Check each requirement in prd.md is implemented
3. **Spec sync** - Analyze whether changes introduce new patterns, contracts, or conventions
   - If new pattern/convention found: read target spec file → update it → update index.md if needed
   - If infra/cross-layer change: follow the 7-section mandatory template from update-spec.md
   - If pure code fix with no new patterns: skip this step
4. **Run final checks** - Execute lint and typecheck
5. **Confirm ready** - Ensure code is ready for PR

## Important Constraints

- You MAY update spec files when gaps are detected (use update-spec.md as guide)
- MUST read the target spec file BEFORE editing (avoid duplicating existing content)
- Do NOT update specs for trivial changes (typos, formatting, obvious fixes)
- If critical CODE issues found, report them clearly (fix specs, not code)
- Verify all acceptance criteria in prd.md are met` :
      `# Check Agent Task

You are the Check Agent in the Multi-Agent Pipeline.

## Your Context

${context}

---

## Your Task

${originalPrompt}

---

## Workflow

1. **Get changes** - Run \`git diff --name-only\` and \`git diff\`
2. **Check against specs** - Check item by item
3. **Self-fix** - Fix issues directly, don't just report
4. **Run verification** - Run lint and typecheck

## Important Constraints

- Fix issues yourself, don't just report
- Must execute complete checklist`,

    debug: `# Debug Agent Task

You are the Debug Agent in the Multi-Agent Pipeline.

## Your Context

${context}

---

## Your Task

${originalPrompt}

---

## Workflow

1. **Understand issues** - Analyze issues pointed out
2. **Locate code** - Find positions that need fixing
3. **Fix against specs** - Fix following dev specs
4. **Verify fixes** - Run typecheck

## Important Constraints

- Do NOT execute git commit unless the user explicitly asks for it
- Run typecheck after each fix`,

    research: `# Research Agent Task

You are the Research Agent in the Multi-Agent Pipeline.

## Core Principle

**You do one thing: find and explain information.**

## Project Info

${context}

---

## Your Task

${originalPrompt}

---

## Workflow

1. **Understand query** - Determine search type and scope
2. **Plan search** - List search steps
3. **Execute search** - Run multiple searches in parallel
4. **Organize results** - Output structured report

## Strict Boundaries

**Only allowed**: Describe what exists, where it is, how it works

**Forbidden**: Suggest improvements, criticize implementation, modify files`
  }

  return templates[agentType] || originalPrompt
}

export default async ({ directory }) => {
  const ctx = new TrellisContext(directory)
  debugLog("inject", "Plugin loaded, directory:", directory)

  return {
    // ==========================================================================
    // ⚠️ KNOWN LIMITATION: OpenCode project-level plugins cannot intercept subagents
    //
    // This hook will NOT be triggered because:
    // 1. Project-level plugins (.opencode/plugin/) don't support tool.execute.before
    // 2. Only global plugins (npm packages) have full hook permissions
    // 3. This is a known OpenCode architecture limitation (see Issue #5894)
    //
    // SOLUTION: Trellis + OpenCode users must install oh-my-opencode (omo)
    // - omo is a global plugin with full hook permissions
    // - omo reads .claude/settings.json and executes Python hooks
    // - .claude/hooks/inject-subagent-context.py handles the actual injection
    //
    // References:
    // - https://github.com/sst/opencode/issues/5894 (plugin hooks don't intercept subagent)
    // - https://github.com/sst/opencode/issues/2588 (subagent inherit context)
    // ==========================================================================
    "tool.execute.before": async (input, output) => {
      try {
        debugLog("inject", "tool.execute.before called, tool:", input?.tool)

        // Only handle Task tool
        const toolName = input?.tool?.toLowerCase()
        if (toolName !== "task") {
          return
        }

        const args = output?.args || {}
        const subagentType = args.subagent_type
        const originalPrompt = args.prompt || ""

        debugLog("inject", "Task tool called, subagent_type:", subagentType)

        // Only handle supported agent types
        if (!AGENTS_ALL.includes(subagentType)) {
          debugLog("inject", "Skipping - unsupported subagent_type")
          return
        }

        // Check if we should skip (omo will handle)
        if (ctx.shouldSkipHook("inject-subagent-context")) {
          debugLog("inject", "Skipping - omo will handle via .claude/hooks/")
          return
        }

        // Read current task
        const taskDir = ctx.getCurrentTask()

        // Agents requiring task directory
        if (AGENTS_REQUIRE_TASK.includes(subagentType)) {
          if (!taskDir) {
            debugLog("inject", "Skipping - no current task")
            return
          }
          const taskDirFull = join(directory, taskDir)
          if (!existsSync(taskDirFull)) {
            debugLog("inject", "Skipping - task directory not found")
            return
          }

          // Update current_phase in task.json
          updateCurrentPhase(ctx, taskDir, subagentType)
        }

        // Check for [finish] marker
        const isFinish = originalPrompt.toLowerCase().includes("[finish]")

        // Get context based on agent type
        let context = ""
        switch (subagentType) {
          case "implement":
            context = getImplementContext(ctx, taskDir)
            break
          case "check":
            // Use finish context for [finish] phase (lighter, focused on final verification)
            // Use check context for regular check (full specs for self-fix loop)
            context = isFinish
              ? getFinishContext(ctx, taskDir)
              : getCheckContext(ctx, taskDir)
            break
          case "debug":
            context = getDebugContext(ctx, taskDir)
            break
          case "research":
            context = getResearchContext(ctx, taskDir)
            break
        }

        if (!context) {
          debugLog("inject", "No context to inject")
          return
        }

        // Build enhanced prompt
        const newPrompt = buildPrompt(subagentType, originalPrompt, context, isFinish)

        // Update the tool input
        output.args = {
          ...args,
          prompt: newPrompt
        }

        debugLog("inject", "Injected context for", subagentType, "prompt length:", newPrompt.length)

      } catch (error) {
        debugLog("inject", "Error in tool.execute.before:", error.message, error.stack)
      }
    }
  }
}
