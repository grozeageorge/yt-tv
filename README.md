# YT-TV: AI-Driven Video Curation Platform

![Java](https://img.shields.org/badge/Java-21-orange.svg)
![Spring Boot](https://img.shields.org/badge/Spring_Boot-3.4-green.svg)
![Spring AI](https://img.shields.org/badge/Spring_AI-Agentic-blueviolet.svg)
![Architecture](https://img.shields.org/badge/Architecture-RAG-blue.svg)
![License](https://img.shields.org/badge/License-MIT-lightgrey.svg)

## Project Overview

YT-TV is a full-stack web application designed to solve "decision fatigue" on YouTube. Instead of actively searching for content, the application simulates a linear TV experience where channels are curated and scheduled automatically.

I built this project to explore **Agentic AI workflows** within the Java ecosystem. Unlike standard chatbots, the AI here acts as a system controller, it has read/write access to the database and can autonomously decide when to query the vector store based on user intent.

##  Demo

### Agentic AI Chat
*The AI autonomously analyzes the request for "Science", queries the Vector DB, and returns a curated queue.*
![AI Cha
t Demo](screenshots/chat-demo.gif)

### Infinite TV Player
*Seamless playback with auto-pagination when the queue ends.*
![Player Demo](screenshots/player-demo.gif)

### Dashboard Interface
*Responsive dark-mode UI for managing channels and playlists.*
![Dashboard](screenshots/dashboard.gif)

## System Architecture

The application follows a **Modular Monolithic** architecture using Spring Boot. I chose a hybrid database approach to balance relational data integrity with semantic search capabilities.

### 1. Hybrid Data Storage
*   **Microsoft SQL Server:** Handles structured relational data (Users, Playlists, Channels, Watch History). I used this to enforce strict foreign key constraints and transactional integrity.
*   **ChromaDB (Vector Store):** Stores vector embeddings of video metadata. This allows for semantic search.

### 2. Retrieval-Augmented Generation (RAG) Pipeline
To make the AI context-aware, I implemented a custom ingestion pipeline:
1.  **Fetch:** The system pulls channel data via the YouTube Data API v3.
2.  **Categorize:** A service uses **Llama 3.1** to analyze the channel's recent video titles and auto-tag the channel into a category (Science, Music, Tech, etc.).
3.  **Embed:** The video metadata and category are embedded using `nomic-embed-text` and stored in ChromaDB.

### 3. Agentic AI ("Nova")
Instead of a simple chatbot, I implemented an Agentic workflow using **Spring AI**.
*   **The Brain:** Llama 3.1 (8B parameter model via Ollama).
*   **The Logic:** The model is provided with a JSON schema of available tools (e.g., `searchVideos`).
*   **The Flow:** When a user types "Show me science videos", the LLM autonomously parses the intent, extracts the "Science" parameter, executes the Java function to query the vector DB, and synthesizes the results into a natural response.

## Key Engineering Challenges

### The "Infinite Scroll" Logic
One of the main challenges was creating a seamless playback loop without hitting YouTube API limits constantly.
*   **Solution:** I implemented a recursive backend logic. When the frontend player finishes a queue of 20 videos, it triggers a callback. The backend checks the SQL `watched_videos` table to filter out consumed content. If the local cache is empty, it uses the YouTube `nextPageToken` to fetch the next batch, persists it, and serves it, creating an infinite loop for the user.

### Data Integrity vs. Vector Search
Deleting a channel in a relational DB is straightforward, but syncing that state with a Vector DB is complex.
*   **Solution:** I implemented a "Safe Delete" transaction. When a channel is removed, the system first clears the relational watch history (to prevent FK violations), deletes the SQL entries, but **retains** the Vector data. This allows the "Community Library" feature, where content added by one user remains discoverable via AI for other users.

## AI Search Pipeline
- Queries are parsed into categories and/or channel names, then a vector search runs on Chroma.
- If strict category/channel filtering returns no matches, the search falls back to semantic results.
- The response text is generated from the matched documents only, and the Play button uses returned video IDs.

## Chroma Data Persistence
- Chroma data is stored in the Docker volume `chroma-data`.
- `docker compose down` keeps data; `docker compose down -v` deletes it.
- Docker Desktop may show `0 bytes` for volumes even when data exists.

## Tech Stack

*   **Backend:** Java 21, Spring Boot 3.4.0
*   **Security:** Spring Security (Stateful session management, BCrypt hashing)
*   **AI:** Spring AI (1.0.3), Llama 3.1, ChromaDB
*   **Frontend:** Thymeleaf, Bootstrap 5, Custom JS
*   **Infrastructure:** Docker (for ChromaDB)

## Getting Started

### Prerequisites
*   Java 21 JDK
*   Docker Desktop (for the Vector Database)
*   Ollama (running locally)
*   Google Cloud API Key (YouTube Data API v3)

### Installation

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/yourusername/yt-tv.git
    ```

2.  **Prepare the AI Models:**
    Run these commands to pull the necessary models into Ollama:
    ```bash
    ollama pull llama3.1
    ollama pull nomic-embed-text
    ```


3.  **Start the Database:**
    ```bash
    docker run -d --name chroma -p 8000:8000 ghcr.io/chroma-core/chroma:1.0.0
    ```

4.  **Configure Environment:**
    Set the API key in your IDE or environment variables:
    ```properties
    GOOGLE_API_KEY=your_key_here
    ```

5.  **Run:**
    ```bash
    mvn spring-boot:run
    ```

## Future Improvements

*   **MCP Server Integration:** I plan to implement a custom MCP server to help the AI agent be more efficent. 
*   **Microservices Migration:** The current modular structure is ready to be split into `auth-service`, `ingestion-service`, and `ai-service` using Spring Cloud for scalability.
*   **AWS Cloud:** Deploying and using AWS services to scale the application and make it more robust.

## License

This project is open source and available under the [MIT License](LICENSE).

---
*Created by Grozea George*