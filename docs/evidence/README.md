# Evidence System

Create one directory per implementation task, e.g. `docs/evidence/TASK-002/`.

Recommended contents:

- `REPORT.md` - concise acceptance narrative and limitations;
- `commands.txt` - exact verification commands;
- `automated-results.txt` - summarized compile/test/lint output;
- `review.md` - independent reviewer verdict;
- `physical.md` - required device/environment protocol and observations;
- small sanitized logs/screenshots only when they materially support acceptance.

Do not commit enormous raw logs, private interiors, stable SSID/BSSID/cell identities, credentials, personal usage history or production secrets.

Reusable synthetic/sanitized regression data belongs in `fixtures/`, not duplicated inside every evidence folder.

A task is complete when its acceptance criteria are supported by evidence. “Agent says it works” is not evidence.
