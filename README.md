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


# Faraway Game

Faraway Game is a digital board game project developed with Java.  
The project includes a graphical user interface, online and offline game modes, player configuration, card gameplay, saving/loading functionality, and game-state management.

## Project Overview

This project implements the board game **Faraway** as a software application.  
The application contains several scenes, including the main menu, game mode selection, settings screens, game scene, tableau view, result screen, and game menu.

The application architecture is organized into GUI, service, and entity layers.

---

## How to Play

> **Note:** The `ESC` key can be used at any time to return to the previous view.

### Main Menu

From the main scene, the player can choose between three options:

- **Start New Game:** starts a new game from the beginning.
- **Join Game:** joins an already running game.
- **Load Game:** loads a saved game state.

![Main Scene](docs/images/how-to-play/main-scene.png)

---

### Join Game

The join screen is used to connect to an existing online game.

- **Simulation Speed:** defines the speed of the game flow:
  - **Zero:** no delay
  - **Low:** fast game
  - **Normal:** normal speed, recommended
  - **Extreme:** very slow game
- **Session ID:** enter the individual code to connect to a game server, for example a friend's server.
- **Player List:** shows the players who are already connected.

![Join Game](docs/images/how-to-play/join-game.png)

---

### Select Game Mode

After selecting **Start New Game**, the **Select Game Mode** screen appears.

- **Online:** starts a new game in online mode. Other players can join using a session ID.
- **Offline:** starts a local game without a network connection.

![Select Game Mode](docs/images/how-to-play/select-game-mode.png)

---

### Online Settings

After selecting online mode, the settings screen appears.

- **Variant:**
  - **Basic:** players start with three cards in their hand.
  - **Professional:** players choose three cards out of five.
- **Simulation Speed:** defines the speed of the game.
- **Players Order:**
  - **Random:** the player order is generated randomly.
  - **Manual:** the player order is configured manually.
- **Session ID:** is created so that other players can join the game.

![Online Settings](docs/images/how-to-play/settings-online.png)

---

### Offline Settings

In offline mode, the settings screen is used to configure the game and add players.

- **Variant:** Basic or Professional.
- **Simulation Speed:** controls the speed of the game flow.
- **Players Order:** defines whether the player order is random or manual.

![Offline Settings](docs/images/how-to-play/settings-offline.png)

---

### Configure Players

By clicking the player icon next to a player slot, a configuration window opens.  
The arrows can be used to change the player type and order.

- **Name:** name of the player.
- **Type:** player type, such as Local, Easy Bot, or Hard Bot.
- **Order:** position of the player in the turn order.

![Configure Player](docs/images/how-to-play/configure-player.png)

---

### Player Overview

The player overview shows all added players.

- Click the **gear icon** to edit a player's settings again.
- Click the **door icon** to remove a player from the game.
- Click the **red arrow** to start the game.

![Player Overview](docs/images/how-to-play/player-overview.png)

---

### Play a Region Card

In the first phase, each player plays one region card from their hand.

- Click a card from the hand and drag it to the **PlayedCard** field.
- After the card is played, the next player takes their turn.
- This phase ends when all players have played one region card.

Additional actions:

- Click the cards on the right side to view the region-card draw stack.
- Click the tableau to view the played cards of all players.
- Use the arrows on the left side to undo or redo actions.

![Play Region Card](docs/images/how-to-play/play-region-card.png)

---

### Tableau View

The tableau view opens automatically in phase 2 for sanctuary selection.  
It can also be opened manually by clicking on the tableau.

- The sanctuary card in the upper-left area can be selected by clicking it.
- After selecting the sanctuary card, it is placed at the bottom.
- The sanctuary card in the upper-right area shows the sanctuary-card draw stack.
- The eight slots in the middle-right area show the played region cards.
- The three cards at the bottom-right area are the player's hand cards.
- The arrows at the top can be used to switch between players and view their tableau.

![Tableau View](docs/images/how-to-play/tableau-view.png)

---

### Choose a Region Card

After selecting a sanctuary card, the player chooses a region card.

- Click the desired region card.
- The selected card is added to the player's hand.
- Each player then has three cards in their hand again.

![Choose Region Card](docs/images/how-to-play/choose-region-card.png)

---

### End of the Game

The game ends automatically after the final round.

- The score overview of all players is displayed.
- The player with the highest total score is shown as the winner.
- By clicking a player name, the tableau overview of that player can be displayed.

---

## GUI Flow

The following diagram shows the graphical user interface flow of the application.  
It describes how the player moves between the main menu, game mode selection, online/offline overlays, waiting screen, game scene, game menu, result screen, and game table view.

![GUI Flow](docs/images/gui-flow.jpg)

---

## Class Diagram

The following class diagram shows the main structure of the application.  
It presents the relationship between the GUI layer, service layer, and entity layer.

![Class Diagram](docs/images/class-diagram.png)

---

## Technologies Used

- Java
- JavaFX
- Object-Oriented Programming
- UML Class Diagrams
- GUI Scene Management

---

## Features

- Main menu with game options
- Online and offline game mode selection
- Game settings with simulation speed and player order
- Player configuration with local players and bots
- Interactive card gameplay
- Tableau view for player cards and sanctuary selection
- Undo and redo actions
- Result screen after game completion
- Structured service and entity layers

---

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
│       ├── class-diagram.png
│       └── how-to-play/
│           ├── main-scene.png
│           ├── join-game.png
│           ├── select-game-mode.png
│           ├── settings-online.png
│           ├── settings-offline.png
│           ├── configure-player.png
│           ├── player-overview.png
│           ├── play-region-card.png
│           ├── tableau-view.png
│           └── choose-region-card.png
│
└── README.md
