# Faraway Game

Faraway Game is a digital board game project developed with Java.  
The game includes a graphical user interface, multiple game scenes, player actions, menus, saving/loading functionality, and game-state management.

## Project Overview

This project focuses on the implementation of the board game **Faraway** as a software application.  
It includes different scenes such as the main menu, game scene, waiting scene, result scene, and game table scene.  
The application architecture is organized into GUI, service, and entity layers.

## GUI Flow

The following diagram shows the graphical user interface flow of the application.  
It describes how the player moves between the main menu, game mode selection, online/offline overlays, waiting screen, game scene, game menu, result screen, and game table view.

![GUI Flow](docs/images/gui-flow.jpg)

## Class Diagram

The following class diagram shows the main structure of the application.  
It presents the relationship between the GUI layer, service layer, and entity layer.  
Important classes include the game scenes, services for player actions and game state management, and core entities such as players, cards, regions, quests, and the Faraway game model.

![Class Diagram](docs/images/class-diagram.png)

## Technologies Used

- Java
- JavaFX
- Object-Oriented Programming
- UML Class Diagrams
- GUI Scene Management

## Features

- Main menu with game options
- Online and offline game mode selection
- Game scene with interactive board elements
- Game menu with resume, save, and exit options
- Result screen after game completion
- Structured service layer for game logic
- Entity layer for players, cards, regions, quests, and game state

## Repository Structure

```text
Faraway-Game/
│
├── src/
│   └── main project source code
│
├── docs/
│   └── images/
│       ├── gui-flow.jpg
│       └── class-diagram.png
│
└── README.md

