Doodler
Real-Time Multiplayer Drawing & Guessing Game
📌 Overview

Doodler is a real-time multiplayer drawing and guessing game inspired by Scribbl.io.
The application enables multiple players to join shared game rooms, draw collaboratively, and guess words in real time using WebSockets for low-latency communication.

The project focuses on real-time synchronization, concurrency handling, and interactive UI design.

🎯 Objective

Build a real-time multiplayer application with minimal latency

Synchronize drawing actions across multiple clients

Manage game rooms and player sessions reliably

Design an interactive and responsive user interface

🧠 System Architecture
1️⃣ Client Interface (JavaFX)

Interactive drawing canvas

Real-time display of other players’ drawings

Guess input and score updates

Room-based gameplay UI

2️⃣ Real-Time Communication (WebSockets)

WebSockets used for:

Broadcasting drawing events

Synchronizing guesses and scores

Managing player join/leave events

Ensures low-latency, bidirectional communication between clients and server

3️⃣ Multiplayer Game Logic

Room-based architecture

Turn-based drawing and guessing

Word selection and scoring logic

Session handling for multiple players

🔄 Real-Time Synchronization

Drawing actions captured as events

Events streamed to all connected clients in the same room

Ensures consistent game state across players

✅ Current Project Status

✔ Multiplayer game rooms
✔ Real-time drawing synchronization
✔ Guessing and scoring logic
✔ JavaFX UI integration

🚧 Future Enhancements

User authentication

Chat moderation

Game persistence

Improved scalability for larger rooms

🔍 Key Learnings

Designing event-driven systems

Handling concurrency in real-time applications

Using WebSockets for low-latency communication

Managing shared state across multiple clients

🛠 Tech Stack

Language: Java

UI Framework: JavaFX

Communication: WebSockets

Architecture: Client–Server

📁 Project Structure
Doodler/
│
├── client/               # JavaFX client
├── server/               # WebSocket server
├── game/                 # Game logic
├── utils/                # Utility classes
└── README.md

📌 Use Case Relevance

Although a game, Doodler demonstrates:

Real-time event handling

Distributed system concepts

State synchronization across users

These concepts are directly applicable to enterprise platforms that require real-time updates and reliability.

📎 Disclaimer

This project is developed for educational purposes and is not a production-scale multiplayer system.

