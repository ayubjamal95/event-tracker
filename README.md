# Event Hotel Impact Tracker MCP Server

Track how events,seasonality, holidays affect hotel prices in real-time.

## Features

- 🎭 Detects major events from Ticketmaster and holidays from Nager
- 🏨 Searches hotels via Trivago MCP server
- 📊 Calculates price surge percentages
- 🤖 Powered by Claude AI for intelligent analysis

## Prerequisites

- Java 17+
- Maven 3.6+
- API Keys for:
    - Ticketmaster

## Setup

1. Clone the repository
2. Update `application.properties` with your API keys
3. Run: `mvn clean package -DskipTests`
4. Deploy the jar file to Claude desktop and enable the connector

## API Keys

### Ticketmaster
Get your key at: https://developer.ticketmaster.com/

## Usage

1. prompt "Analyize the price surge in" a city (e.g., "Chicago, IL")
2. Select check-in and check-out dates of your choosing 
4. Claude will give comprehensive analysis of the surge

## Tech Stack

- Spring Boot 3.2
- WebFlux (WebClient)
- Lombok
- Jackson
- Claude Desktop

## License

MIT