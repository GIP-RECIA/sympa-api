## Setup git hooks

`npm install`

`git config core.hooksPath .githooks`

Due to PATH limitations in some GUI tools, use the terminal to commit.
You can still use the GUI to stage or review changes.

---

## Commandes pour notice et license

- `mvn -f ../pom.xml notice:check`
- `mvn -f ../pom.xml notice:generate`
- `mvn -f ../pom.xml license:check`
- `mvn -f ../pom.xml license:format`
- `mvn -f ../pom.xml license:remove`


