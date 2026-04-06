# Contributing to Linkora

Thanks for your interest in improving Linkora. To keep things from turning into a mess, please
follow these guidelines.

## Check the Label

* **"Open For Contribution"**: These are safe to pick up and won't conflict with my current work.
* **No Label**: These are usually things I want to handle myself. They might not be in active
  development right now, but I am still interested in building them. You can comment on the issue or
  email me, and if I am not working on it, I will assign it to you.

## How to Start

1. **Find a Task:** Look for the "Open For Contribution" label.
2. **Claim It:** Comment on the issue so we don't end up duplicating work.
3. **Propose Something New:** If your idea isn't already in the Issues, open one first. Please don't
   write any code until I confirm it's something I want in the app. I have an unhealthy habit of
   refactoring and rewriting things for no reason, so confirming upfront avoids your work colliding
   with mine and creating a messy merge conflict.

## Development Guidelines

**Coding style:** Try to match the existing code style. Keeping things consistent prevents
unnecessary refactors. If your code looks like it naturally belongs here, you are good to go.

**dev/feature branch:** Make sure to use a `dev/feat` branch instead of working directly on
`master`. Only pull upstream changes into `master`.

**Rebase instead of merging:** When working on a specific branch, never merge changes from `master`.
Instead, rebase them. This keeps your changes on top, maintains a linear history, and avoids a
tangled commit graph.

## Submitting a Pull Request

* **One issue = one PR.** Keeping them separate makes merging much easier.
* **Clear titles.** Describe exactly what your PR does.
* **Verify it works.** Since Linkora is built with Kotlin Multiplatform, you **must** test your
  changes on **Android, Desktop, and Web** before submitting.
* **Tests.** Adding test cases for important blocks of code within your PR is highly appreciated.
* **Description and media.** Make sure to reference the original issue. If your PR changes the UI,
  please attach screenshots or a screen recording.

## Questions?

Feel free to ask in **Discussions**, drop a comment on the relevant issue, or email me directly at
sakethh@proton.me.