#!/usr/bin/env python3
"""
Playlist Vibe Server
====================
TCP-Server der eine LLM (Ollama / OpenAI-kompatibel) nutzt um aus einer
Song-Liste eine vibe-basierte Playlist zu filtern.

Protokoll (JSON über TCP):
  Client sendet:
  {
    "vibe": "Chill Jazz Café",
    "songs": [
      {"artist": "Miles Davis", "title": "So What", "id": 1},
      {"artist": "Slayer", "title": "Raining Blood", "id": 2},
      ...
    ]
  }

  Server antwortet:
  {
    "vibe": "Chill Jazz Café",
    "playlist": [
      {"artist": "Miles Davis", "title": "So What", "id": 1},
      ...
    ]
  }

Nachrichten werden length-prefixed gesendet:
  4 Bytes (big-endian uint32) = Länge der JSON-Nachricht in Bytes
  danach die JSON-Nachricht als UTF-8
"""

import json
import struct
import socket
import threading
import logging
import argparse
import sys
from typing import Any

import requests  # für Ollama HTTP API

try:
    from duckduckgo_search import DDGS
    HAS_DDG = True
except ImportError:
    HAS_DDG = False

# ─── Konfiguration ───────────────────────────────────────────────────────────

DEFAULT_HOST = "0.0.0.0"
DEFAULT_PORT = 9550
DEFAULT_LLM_URL = "http://localhost:11434"  # Ollama default
DEFAULT_MODEL = "llama3"  # oder mistral, gemma2, etc.
DEFAULT_WEBSEARCH = True  # Web-Suche standardmäßig aktiviert

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
)
log = logging.getLogger("playlist-server")

# ─── Web-Suche (DuckDuckGo) ─────────────────────────────────────────────────

def search_vibe_context(vibe: str, max_results: int = 5) -> str:
    """
    Sucht im Web nach Infos zur gewünschten Stimmung/Vibe.
    Gibt einen Kontext-String zurück den die LLM nutzen kann.
    """
    if not HAS_DDG:
        log.warning("duckduckgo-search nicht installiert! pip install duckduckgo-search")
        return ""

    queries = [
        f"{vibe} playlist songs typical",
        f"{vibe} music recommendations",
    ]

    results = []
    try:
        with DDGS() as ddgs:
            for query in queries:
                log.info("🔍 Web-Suche: '%s'", query)
                hits = list(ddgs.text(query, max_results=max_results))
                for hit in hits:
                    title = hit.get("title", "")
                    body = hit.get("body", "")
                    results.append(f"- {title}: {body}")
    except Exception as e:
        log.warning("Web-Suche fehlgeschlagen: %s", e)
        return ""

    if not results:
        return ""

    context = "\n".join(results[:10])  # Max 10 Ergebnisse
    log.info("🔍 %d Web-Ergebnisse gefunden für Vibe '%s'", len(results), vibe)
    return context


# ─── LLM Kommunikation ──────────────────────────────────────────────────────

SYSTEM_PROMPT = """\
Du bist ein Musik-Kurator. Du bekommst eine Liste von Songs (als JSON-Array) \
und eine gewünschte Stimmung/Vibe.

Dir werden auch Web-Suchergebnisse bereitgestellt, die beschreiben welche Art \
von Musik zu dieser Stimmung passt. Nutze diese Informationen um bessere \
Entscheidungen zu treffen, welche Songs passen.

Deine Aufgabe:
1. Wähle NUR Songs aus der gegebenen Liste aus, die zur gewünschten Stimmung passen.
2. Gib die ausgewählten Songs als JSON-Array zurück – exakt im selben Format \
   wie sie dir gegeben wurden (gleiche Felder, gleiche Werte).
3. Antworte NUR mit dem JSON-Array. Kein Erklärungstext, kein Markdown, \
   keine Code-Fences. Nur valides JSON.
4. Wenn kein Song passt, antworte mit einem leeren Array: []
5. Erfinde KEINE neuen Songs. Verwende nur Songs aus der gegebenen Liste.
"""

SYSTEM_PROMPT_NO_SEARCH = """\
Du bist ein Musik-Kurator. Du bekommst eine Liste von Songs (als JSON-Array) \
und eine gewünschte Stimmung/Vibe.

Deine Aufgabe:
1. Wähle NUR Songs aus der gegebenen Liste aus, die zur gewünschten Stimmung passen.
2. Gib die ausgewählten Songs als JSON-Array zurück – exakt im selben Format \
   wie sie dir gegeben wurden (gleiche Felder, gleiche Werte).
3. Antworte NUR mit dem JSON-Array. Kein Erklärungstext, kein Markdown, \
   keine Code-Fences. Nur valides JSON.
4. Wenn kein Song passt, antworte mit einem leeren Array: []
5. Erfinde KEINE neuen Songs. Verwende nur Songs aus der gegebenen Liste.
"""


def query_llm(
    vibe: str,
    songs: list[dict],
    llm_url: str,
    model: str,
    use_websearch: bool = True,
) -> list[dict]:
    """Fragt die LLM via Ollama /api/chat, optional mit Web-Suche Kontext."""

    # ── Web-Suche für besseren Kontext ──
    web_context = ""
    if use_websearch:
        web_context = search_vibe_context(vibe)

    songs_json = json.dumps(songs, ensure_ascii=False)

    if web_context:
        user_msg = (
            f"Gewünschte Stimmung/Vibe: \"{vibe}\"\n\n"
            f"── Web-Recherche zu dieser Stimmung ──\n"
            f"{web_context}\n\n"
            f"── Verfügbare Songs ──\n{songs_json}\n\n"
            f"Nutze die Web-Recherche um zu verstehen welche Musik zur Stimmung "
            f"passt, aber wähle NUR Songs aus der obigen Liste.\n"
            f"Antworte NUR mit dem JSON-Array der passenden Songs."
        )
        system_prompt = SYSTEM_PROMPT
    else:
        user_msg = (
            f"Gewünschte Stimmung/Vibe: \"{vibe}\"\n\n"
            f"Verfügbare Songs:\n{songs_json}\n\n"
            f"Antworte NUR mit dem JSON-Array der passenden Songs."
        )
        system_prompt = SYSTEM_PROMPT_NO_SEARCH

    # Ollama /api/chat Endpoint
    payload = {
        "model": model,
        "messages": [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_msg},
        ],
        "stream": False,
        "options": {
            "temperature": 0.3,  # niedrig für konsistente Ergebnisse
        },
    }

    try:
        resp = requests.post(
            f"{llm_url}/api/chat",
            json=payload,
            timeout=300,  # LLMs können langsam sein
        )
        resp.raise_for_status()
        data = resp.json()
        content = data["message"]["content"].strip()
    except requests.RequestException as e:
        log.error("LLM request fehlgeschlagen: %s", e)
        raise RuntimeError(f"LLM nicht erreichbar: {e}") from e

    # Parse Antwort – LLM gibt manchmal Markdown-Fences zurück
    content = content.strip()
    if content.startswith("```"):
        # Entferne ```json ... ``` Wrapper
        lines = content.split("\n")
        lines = [l for l in lines if not l.strip().startswith("```")]
        content = "\n".join(lines).strip()

    try:
        result = json.loads(content)
    except json.JSONDecodeError:
        log.warning("LLM Antwort ist kein valides JSON: %s", content[:200])
        # Fallback: versuche JSON-Array aus der Antwort zu extrahieren
        import re
        match = re.search(r'\[.*\]', content, re.DOTALL)
        if match:
            try:
                result = json.loads(match.group())
            except json.JSONDecodeError:
                log.error("Konnte kein JSON aus LLM-Antwort extrahieren")
                return []
        else:
            return []

    if not isinstance(result, list):
        log.warning("LLM hat kein Array zurückgegeben, sondern: %s", type(result))
        return []

    # Validierung: nur Songs zurückgeben die tatsächlich in der Eingabe waren
    valid_ids = {s.get("id") for s in songs if "id" in s}
    valid_titles = {(s.get("artist", "").lower(), s.get("title", "").lower()) for s in songs}

    filtered = []
    for s in result:
        if not isinstance(s, dict):
            continue
        # Prüfe via ID oder Artist+Title
        if "id" in s and s["id"] in valid_ids:
            # Gib das Original zurück, nicht die LLM-Version (sicherer)
            original = next((orig for orig in songs if orig.get("id") == s["id"]), None)
            if original:
                filtered.append(original)
        elif (s.get("artist", "").lower(), s.get("title", "").lower()) in valid_titles:
            original = next(
                (orig for orig in songs
                 if orig.get("artist", "").lower() == s.get("artist", "").lower()
                 and orig.get("title", "").lower() == s.get("title", "").lower()),
                None,
            )
            if original:
                filtered.append(original)

    # Duplikate entfernen (stabile Reihenfolge)
    seen = set()
    unique = []
    for s in filtered:
        key = s.get("id", (s.get("artist"), s.get("title")))
        hkey = str(key)
        if hkey not in seen:
            seen.add(hkey)
            unique.append(s)

    return unique


# ─── TCP Protokoll Helpers ───────────────────────────────────────────────────

def send_msg(sock: socket.socket, data: dict) -> None:
    """Sendet eine JSON-Nachricht mit 4-Byte Längen-Prefix."""
    raw = json.dumps(data, ensure_ascii=False).encode("utf-8")
    sock.sendall(struct.pack("!I", len(raw)) + raw)


def recv_msg(sock: socket.socket) -> dict | None:
    """Empfängt eine length-prefixed JSON-Nachricht."""
    header = _recv_exact(sock, 4)
    if not header:
        return None
    (length,) = struct.unpack("!I", header)
    if length > 50_000_000:  # 50 MB Limit
        raise ValueError(f"Nachricht zu groß: {length} Bytes")
    raw = _recv_exact(sock, length)
    if not raw:
        return None
    return json.loads(raw.decode("utf-8"))


def _recv_exact(sock: socket.socket, n: int) -> bytes | None:
    """Empfängt exakt n Bytes."""
    buf = bytearray()
    while len(buf) < n:
        chunk = sock.recv(n - len(buf))
        if not chunk:
            return None
        buf.extend(chunk)
    return bytes(buf)


# ─── Client Handler ──────────────────────────────────────────────────────────

def handle_client(
    conn: socket.socket,
    addr: tuple,
    llm_url: str,
    model: str,
    use_websearch: bool = True,
) -> None:
    """Bearbeitet einen einzelnen Client."""
    log.info("Client verbunden: %s:%d", addr[0], addr[1])
    try:
        msg = recv_msg(conn)
        if msg is None:
            log.warning("Client hat keine Daten gesendet")
            return

        vibe = msg.get("vibe", "")
        songs = msg.get("songs", [])

        if not vibe:
            send_msg(conn, {"error": "Kein 'vibe' angegeben"})
            return
        if not songs:
            send_msg(conn, {"error": "Keine 'songs' angegeben"})
            return

        log.info(
            "Request: vibe='%s', %d Songs von %s:%d",
            vibe, len(songs), addr[0], addr[1],
        )

        playlist = query_llm(vibe, songs, llm_url, model, use_websearch)

        response = {
            "vibe": vibe,
            "playlist": playlist,
        }
        send_msg(conn, response)
        log.info(
            "Response: %d/%d Songs für vibe='%s' an %s:%d",
            len(playlist), len(songs), vibe, addr[0], addr[1],
        )

    except Exception as e:
        log.exception("Fehler bei Client %s:%d", addr[0], addr[1])
        try:
            send_msg(conn, {"error": str(e)})
        except Exception:
            pass
    finally:
        conn.close()
        log.info("Client getrennt: %s:%d", addr[0], addr[1])


# ─── Server ──────────────────────────────────────────────────────────────────

def run_server(host: str, port: int, llm_url: str, model: str, use_websearch: bool = True) -> None:
    """Startet den TCP-Server (ein Thread pro Client)."""
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind((host, port))
    server.listen(5)

    log.info("=" * 60)
    log.info("Playlist Vibe Server gestartet")
    log.info("  TCP:        %s:%d", host, port)
    log.info("  LLM:        %s (model: %s)", llm_url, model)
    log.info("  Web-Suche:  %s", "AN ✅" if use_websearch else "AUS ❌")
    if use_websearch and not HAS_DDG:
        log.warning("  ⚠️  duckduckgo-search nicht installiert!")
        log.warning("     pip install duckduckgo-search")
    log.info("=" * 60)

    try:
        while True:
            conn, addr = server.accept()
            t = threading.Thread(
                target=handle_client,
                args=(conn, addr, llm_url, model, use_websearch),
                daemon=True,
            )
            t.start()
    except KeyboardInterrupt:
        log.info("Server wird beendet...")
    finally:
        server.close()


# ─── Main ────────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(description="Playlist Vibe Server")
    parser.add_argument("--host", default=DEFAULT_HOST, help=f"Bind address (default: {DEFAULT_HOST})")
    parser.add_argument("--port", type=int, default=DEFAULT_PORT, help=f"TCP Port (default: {DEFAULT_PORT})")
    parser.add_argument("--llm-url", default=DEFAULT_LLM_URL, help=f"Ollama URL (default: {DEFAULT_LLM_URL})")
    parser.add_argument("--model", default=DEFAULT_MODEL, help=f"LLM Model (default: {DEFAULT_MODEL})")
    parser.add_argument("--no-search", action="store_true", help="Web-Suche deaktivieren")
    args = parser.parse_args()

    run_server(args.host, args.port, args.llm_url, args.model, not args.no_search)


if __name__ == "__main__":
    main()