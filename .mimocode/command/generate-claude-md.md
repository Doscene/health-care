---
description: Generate a CLAUDE.md file for the current codebase
agent: main
---

# Generate CLAUDE.md

Analyze the codebase and create a CLAUDE.md file that provides guidance for future AI coding sessions.

## Input

$ARGUMENTS or $1 — Optional path to project root. Defaults to current directory.

## Workflow

### 1. Explore Project Structure

- Read top-level files (README.md, package.json, go.mod, etc.)
- Identify project type and language
- Map directory structure
- Find build/test/lint commands

### 2. Identify Key Patterns

- Module organization and naming conventions
- Error handling patterns
- API design patterns (REST, GraphQL, etc.)
- Database/ORM usage
- Configuration management

### 3. Document Project Context

- Project purpose and goals
- Technology stack
- Directory structure with explanations
- Key commands for development
- Environment dependencies

### 4. Document Rules and Constraints

- Code style conventions
- Naming conventions
- Import/export patterns
- Testing requirements
- Deployment considerations

### 5. Write CLAUDE.md

Create `.claude/CLAUDE.md` (or `CLAUDE.md` at project root) with:

```markdown
# CLAUDE.md

This file provides guidance for AI coding sessions working in this repository.

## Project Context
[Project description, purpose, goals]

## Directory Structure
[Key directories and their purposes]

## Development Commands
[Build, test, lint, run commands]

## Code Conventions
[Style, naming, patterns]

## Key Patterns
[Architecture decisions, common patterns]

## Environment
[Dependencies, configuration, setup]
```

## Output

- File: `.claude/CLAUDE.md` or `CLAUDE.md`
- Length: 100-300 lines (focused, not exhaustive)
- Tone: Imperative, actionable, concise
