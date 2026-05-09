# Card Staircase Game

Card Staircase Game is a digital two-player card game implemented in Kotlin.  
The project focuses on clean architecture, structured game logic, and an interactive graphical user interface.

---

## Project Overview

This project implements the card game "Card Staircase" as a software application.  
Two players compete by strategically using their cards to gain points through different actions such as combining cards, destroying cards, drawing, and discarding.

The objective of the game is to achieve the highest score by the end of the match.

The application is designed with a clear separation of concerns, divided into:

- GUI layer for user interaction  
- Service layer for game logic  
- Entity layer for core game objects  

---

## Game Concept

- The game is played by two players  
- Each player has a hand of cards  
- A central "staircase" of cards forms the main playfield  
- Players interact with the staircase to gain points  
- The game ends when no more valid moves are possible or the staircase is cleared  

Key mechanics include:

- Combining cards to earn points  
- Destroying cards at a cost  
- Drawing new cards from the deck  
- Discarding cards to manage the hand  

---

## GUI Flow

The following diagram shows the graphical user interface flow of the application.  
It illustrates how players navigate through the different scenes of the game.

![GUI Flow](https://github.com/ahmed-ghroubi/Card-Game-Project/blob/main/GUI%20Flow.png)

---

## Class Diagram

The following class diagram illustrates the overall architecture of the application.  
It shows the relationships between the GUI layer, service layer, and entity layer.

![Class Diagram](https://github.com/ahmed-ghroubi/Card-Game-Project/blob/main/Card_Staircase_Class_Diagram.png)

---

## Technologies Used

- Kotlin
- JavaFX
- Object-Oriented Programming
- UML Class Diagrams
- GUI Scene Management

---

## Features

- Two-player gameplay  
- Interactive graphical user interface  
- Structured game-state management  
- Multiple strategic actions  
- Game history tracking  
- Clear modular architecture  

---

## Repository Structure

```text
Card-Game-Project/
│
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   ├── entity/
│   │   │   ├── gui/
│   │   │   ├── service/
│   │   │   └── Main.kt
│   │   │
│   │   └── resources/
│   │
│   └── test/
│       └── kotlin/
│           ├── entity/
│           └── service/
│
├── Card_Staircase_Class_Diagram.png
├── GUI Flow.png
├── README.md
