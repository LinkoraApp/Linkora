# Contributing to Linkora

Thanks for your interest in improving Linkora.

This is the rulebook for working with this repository and how to get your
work merged. For development, please go through `DEVELOPMENT.md`.

- "Open For Contribution" label = safe to pick up, won't collide with what I'm doing.
- No label = I want to handle it myself, or I still care about it even if I'm not actively on it
  right now. Comment on the issue or email me. If I'm not working on it, I'll assign it to you.
- Got an idea that's not already an issue? Open one first. Don't write code before I confirm I want
  it in the app. I rewrite and refactor things more than I should, and confirming upfront avoids
  your work colliding with mine.
- Match the existing code style. If your code looks like it belongs here, you're good.
- Use a `dev/feat` branch, not `master`. Only pull upstream changes into `master`.
- Rebase instead of merging when pulling in changes from `master`. Keeps history linear, no tangled
  commit graph.
- Commit messages must be written in the imperative tone and strictly in lowercase, unless referring
  to a file, function, or any property that directly references the code. No other convention style
  is allowed. While older commits in the repository might not follow this, it is mandatory moving
  forward.

  Follow the format outlined
  here: https://gist.github.com/qoomon/5dfcdf8eec66a051ecd85625518cfd13?permalink_comment_id=5893039#gistcomment-5893039

  Standard:

  `chore: add package-lock.json (for web worker), yarn.lock to fix F-Droid reproducible builds`

  `feat: add support for refreshing link properties individually`

  `feat: use current system time epoch when importing the data`

  Scoping:

  If scoping makes sense for the commit, include it:

  `feat(android): show confirmation dialog before changing app icon`

  Body:

  If the commit requires a detailed explanation, include a body separated by a blank line:
   ```
  
  fix(android): use BundledSQLiteDriver while building Room instance
  
  Switching to the bundled driver to ensure `json_object` and other JSON1 functions
  are available on all devices.
  
  The version which Waydroid currently is on simply doesn't want to work as there
  are no definitions of what we are looking for in the system, so we gotta pull
  our own thing here.
  ```
  The body of the commit message can be written in either strict lowercase or standard sentence
  case.

- One issue = one PR.
- Clear titles. Describe exactly what the PR does.
- Linkora is KMP. Test your changes on Android, Desktop, and Web before submitting.
- macOS doesn't have an official build yet, and I don't have plans for one right now. Same with iOS,
  I don't own any Apple hardware, so testing and maintaining either platform myself isn't realistic.
  If you want to take on extending support for iOS, I appreciate it, but that's not something I want
  to maintain, so no PR for it will be accepted or merged.
- If you're working on/with a core feature, it needs tests, and they need to pass locally. Run
  `./gradlew desktopTest` at the project root. Everything has to pass, including whatever new tests
  you wrote for your change.
- Reference the original issue in your PR. If it changes the UI, attach screenshots or a recording.
- Adding tests for important code is appreciated even outside core features.
- On AI: I don't want AI-written slop in this repo. If I wanted slop, I'd write it myself.
  Brainstorming with AI is fine. Having it write the actual code or PR without you understanding
  what it's doing is not. I can't tell from a diff alone whether something's AI-written, so if a PR
  looks off during review, I'll ask why you did it a certain way. If it looks correct, I probably
  won't catch it, I don't have a way to verify that either way. But if it does come up and your
  answers don't hold up to someone who actually wrote the code, you're permanently banned from
  contributing here.

Questions? Ask in Discussions, comment on the relevant issue, or email me at sakethh@proton.me.