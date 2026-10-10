# Homestay Booking System

Full-stack Web App (API + SPA) for Hospitality / Property Management / Booking.

## Project Structure & Architecture

This project strictly follows the **Spec-Driven & Agent-Driven Development (SDD + ADD)** methodology.

- **`AGENTS.md`**: Persona and capabilities for AI agents.
- **`CLAUDE.md`**: Project memory, architectural decisions, and context.
- **`CONSTITUTION.md`**: Immutable project laws and coding standards.
- **`sdd/`**: Spec-as-Code artifacts containing modules and features.
- **`docs/`**: UI screens, entity mapping, testing, etc.

For comprehensive guidelines on contributing using AI agents, please read [CONTRIBUTING.md](CONTRIBUTING.md).

## Tech Stack

- **Backend**: Java 17 + Spring Boot 3.x + Spring Security + Spring Data JPA + Maven
- **Frontend**: React 18 + TypeScript + Vite + React Router + Axios + TanStack React Query + Zustand
- **Database**: SQL Server
- **Styling**: Tailwind CSS 3.x + custom UI under `src/components/ui` (shadcn-style)
