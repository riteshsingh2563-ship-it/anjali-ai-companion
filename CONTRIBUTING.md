# Contributing to Anjali AI 🌸

Thank you for your interest in contributing to Anjali AI!

## Code of Conduct
Please be polite, respectful, and collaborative.

## Getting Started
1. Fork the repository on GitHub.
2. Clone your fork locally:
   ```bash
   git clone https://github.com/<your-username>/anjali-ai-companion.git
   cd anjali-ai-companion
   ```
3. Create a branch for your feature or bugfix:
   ```bash
   git checkout -b feature/my-new-feature
   ```

## Development Guidelines
* **Android App:** Ensure code compiles cleanly with `./gradlew assembleDebug` and conforms to Android Kotlin style guidelines.
* **Admin Dashboard:** Ensure Next.js and TypeScript checks pass: `cd admin-dashboard && npm run lint`.
* **Security:** Never commit real API keys, `.env` files, or `google-services.json` credentials to the repository.

## Submitting Pull Requests
1. Push your branch to GitHub.
2. Open a Pull Request against the `main` branch.
3. Describe what changed, why, and how you tested your changes.
