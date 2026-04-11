# 🏗️ Arrow Kotlin Repository Conventions

This document defines the professional standards and governance for the Arrow Kotlin repository. Follow these rules to ensure consistent delivery and automated versioning.

---

## 🛰️ Issue Tracking

We follow a strict **AK-###** naming pattern.

- **Issue Naming**: `AK-###: [Type]: [Subject]`
- **Mandatory Labels**:
  - `type:feature` or `enhancement` -> Triggers a **Minor Version Bump**.
  - `type:bug` -> Triggers a **Patch Version Bump**.

---

## 🛠️ Git Workflow

### 1. Branch Naming

- **Pattern**: `[category]/AK-###-[short-description]`
- **Categories**: `feature/`, `fix/`, `chore/`, `docs/`.

### 2. Commit Messages

- **Format**: `type(scope): AK-### [summary] (Closes #ID)`
- **Example**: `feat(ui): AK-001 initial project structure (Closes #1)`

---

## 📦 Automated Release & Versioning

- **GitHub Release Entry**: Every Git tag (vX.X.X) must have a corresponding, formal **GitHub Release** entry.
- **Release Assets**: Formal releases should include the App Bundle (.aab) and Universal APK (.apk).
