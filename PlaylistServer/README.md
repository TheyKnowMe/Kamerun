# 🎵 Playlist Vibe Server

TCP-Server der eine lokale LLM (via **Ollama**) nutzt, um aus einer Song-Liste
eine stimmungsbasierte Playlist zu erstellen.

## Architektur

```
┌──────────┐    TCP (Port 9550)    ┌──────────────┐    HTTP    ┌─────────┐
│  Client   │ ──────────────────▶  │  Vibe Server  │ ────────▶ │  Ollama │
│  (Songs   │ ◀──────────────────  │  (Python)     │ ◀──────── │  (LLM)  │
│  + Vibe)  │    Filtered Songs    │               │  Filtered │         │
└──────────┘                       └──────────────┘   Songs    └─────────┘
```

## Setup

### 1. Ollama installieren

```bash
# Linux
curl -fsSL https://ollama.ai/install.sh | sh

# macOS
brew install ollama

# Dann ein Modell herunterladen:
ollama pull llama3          # 8B – gut für die meisten Systeme
# oder
ollama pull mistral         # 7B – auch gut
# oder
ollama pull gemma2          # 9B
```

### 2. Python-Dependencies

```bash
pip install requests
```

### 3. Server starten

```bash
# Ollama muss laufen (startet automatisch oder):
ollama serve

# In einem neuen Terminal:
python server.py

# Mit Optionen:
python server.py --port 8888 --model mistral --llm-url http://192.168.1.50:11434
```

### 4. Client verwenden

```bash
# Mit Beispiel-Songs:
python client.py --vibe "Jazz Café"
python client.py --vibe "Party Banger"
python client.py --vibe "Melancholisch und Ruhig"
python client.py --vibe "Streamer Gaming Session"

# Mit eigenen Songs (JSON-Datei):
python client.py --vibe "Chill" --file meine_songs.json

# Remote-Server:
python client.py --vibe "Rock" --host 192.168.1.100 --port 9550
```

## Song-Format (JSON)

Jeder Song braucht mindestens `artist` und `title`. `id` ist optional aber empfohlen:

```json
[
  {"id": 1, "artist": "Miles Davis", "title": "So What"},
  {"id": 2, "artist": "Metallica", "title": "Enter Sandman"},
  {"id": 3, "artist": "Norah Jones", "title": "Don't Know Why"}
]
```

Du kannst beliebige Extra-Felder hinzufügen (genre, year, album, etc.) –
sie werden 1:1 durchgereicht.

## Protokoll

TCP mit **Length-Prefix**:
- 4 Bytes Big-Endian uint32 = Länge der JSON-Nachricht
- Danach die JSON-Nachricht als UTF-8

**Request:**
```json
{
  "vibe": "Jazz Café",
  "songs": [...]
}
```

**Response:**
```json
{
  "vibe": "Jazz Café",
  "playlist": [...]
}
```

## Optionen

| Flag | Default | Beschreibung |
|------|---------|-------------|
| `--host` | `0.0.0.0` | Bind-Adresse (Server) / Ziel-Adresse (Client) |
| `--port` | `9550` | TCP Port |
| `--llm-url` | `http://localhost:11434` | Ollama API URL |
| `--model` | `llama3` | Ollama Modell-Name |
| `--vibe` | (required) | Gewünschte Stimmung (nur Client) |
| `--file` | (optional) | JSON-Datei mit Songs (nur Client) |

## Tipps

- **Mehr Songs = bessere Auswahl**, aber auch längere LLM-Verarbeitung
- Bei >500 Songs: batchen oder ein schnelleres Modell nehmen
- `temperature: 0.3` im Server sorgt für konsistente Ergebnisse
- Der Server validiert dass nur Songs aus der Eingabe zurückkommen (keine Halluzinationen)
