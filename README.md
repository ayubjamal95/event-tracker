# Event Hotel Impact Tracker

Track how concerts, sports events, and conferences affect hotel prices in real-time.

## Features

- 🎭 Detects major events from Ticketmaster
- 🏨 Searches hotels via Trivago MCP server
- 📊 Calculates price surge percentages
- 💡 Suggests cheaper alternatives in nearby areas
- 🤖 Powered by Claude AI for intelligent analysis

## Prerequisites

- Java 17+
- Maven 3.6+
- API Keys for:
    - Ticketmaster
    - Anthropic Claude

## Setup

1. Clone the repository
2. Update `application.properties` with your API keys
3. Run: `mvn spring-boot:run`
4. Open: `http://localhost:8080`

## API Keys

### Ticketmaster
Get your key at: https://developer.ticketmaster.com/

### Anthropic Claude
Get your key at: https://console.anthropic.com/

## Usage

1. Enter a city (e.g., "Chicago, IL")
2. Select check-in and check-out dates
3. Click "Analyze Impact"
4. View events, price comparisons, and alternatives

## Tech Stack

- Spring Boot 3.2
- Thymeleaf
- WebFlux (WebClient)
- Lombok
- Jackson

## License

MIT