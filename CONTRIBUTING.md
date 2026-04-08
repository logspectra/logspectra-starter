# Contributing to LogSpectra Starter

Thanks for contributing.

## Before you start (required)

1. Search existing issues to avoid duplicates.
2. Open a new issue describing the bug/feature/proposal.
3. Wait for maintainer confirmation before opening a PR.

Pull requests opened without a prior issue may be closed and redirected to issue discussion first.

## Development setup

Requirements:

- Java `21`
- Maven `3.9+`

Build and test:

```powershell
mvn clean test
mvn clean package
```

## Branching and commits

- Create a branch from `main`.
- Keep commits focused and small.
- Use clear commit messages.

Example:

```text
fix(logback): avoid duplicate kafka appender registration
```

## Pull request checklist

- Linked issue number in PR description (required)
- Tests added/updated for behavior changes
- No breaking API/config changes without discussion
- Documentation updated (`README.md` / config docs) when relevant

## Coding and compatibility guidelines

- Keep Java compatibility at `21`.
- Prefer backward-compatible configuration changes.
- Avoid forcing consumers to replace their local logging config unless necessary.
- Preserve starter behavior when used as a dependency in other services.

## Licensing for contributions

By submitting a contribution, you agree your changes are provided under the Apache License 2.0 for this repository.

If you add third-party code or assets, include proper attribution updates in `NOTICE` when required.

## Reporting security issues

Please do not open public issues for sensitive vulnerabilities. Contact maintainers privately if security contact details are available.
