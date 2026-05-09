#!/usr/bin/env python3
"""
Playlist Vibe Client
====================
Beispiel-Client der eine Song-Liste und einen Vibe an den Server schickt
und die gefilterte Playlist zurückbekommt.

Verwendung:
    python client.py --vibe "Chill Jazz Café"
    python client.py --vibe "Party Banger" --host 192.168.1.100
    python client.py --vibe "Relaxing Acoustic" --file my_songs.json
"""

import json
import struct
import socket
import argparse
import sys

DEFAULT_HOST = "127.0.0.1"
DEFAULT_PORT = 9550

# ─── Beispiel-Songs (wenn kein File angegeben) ──────────────────────────────

EXAMPLE_SONGS = [
    {"id": 1,  "artist": "Miles Davis",       "title": "So What"},
    {"id": 2,  "artist": "Norah Jones",        "title": "Don't Know Why"},
    {"id": 3,  "artist": "Slayer",             "title": "Raining Blood"},
    {"id": 4,  "artist": "Metallica",          "title": "Enter Sandman"},
    {"id": 5,  "artist": "Bill Evans",         "title": "Waltz for Debby"},
    {"id": 6,  "artist": "Chet Baker",         "title": "My Funny Valentine"},
    {"id": 7,  "artist": "Daft Punk",          "title": "Get Lucky"},
    {"id": 8,  "artist": "The Weeknd",         "title": "Blinding Lights"},
    {"id": 9,  "artist": "Bon Iver",           "title": "Skinny Love"},
    {"id": 10, "artist": "Iron Maiden",        "title": "The Trooper"},
    {"id": 11, "artist": "Thelonious Monk",    "title": "Round Midnight"},
    {"id": 12, "artist": "Billie Eilish",      "title": "Ocean Eyes"},
    {"id": 13, "artist": "AC/DC",              "title": "Thunderstruck"},
    {"id": 14, "artist": "Radiohead",          "title": "Creep"},
    {"id": 15, "artist": "Nina Simone",        "title": "Feeling Good"},
    {"id": 16, "artist": "Fleetwood Mac",      "title": "Dreams"},
    {"id": 17, "artist": "Kendrick Lamar",     "title": "HUMBLE."},
    {"id": 18, "artist": "Amy Winehouse",      "title": "Back to Black"},
    {"id": 19, "artist": "John Coltrane",      "title": "A Love Supreme"},
    {"id": 20, "artist": "Lizzo",              "title": "Good as Hell"},
    {"id": 21, "artist": "Frank Sinatra",      "title": "Fly Me to the Moon"},
    {"id": 22, "artist": "Deadmau5",           "title": "Strobe"},
    {"id": 23, "artist": "Adele",              "title": "Someone Like You"},
    {"id": 24, "artist": "Led Zeppelin",       "title": "Stairway to Heaven"},
    {"id": 25, "artist": "Khruangbin",         "title": "Time (You and I)"},
]


# ─── TCP Protokoll Helpers (identisch zum Server) ───────────────────────────

def send_msg(sock: socket.socket, data: dict) -> None:
    raw = json.dumps(data, ensure_ascii=False).encode("utf-8")
    sock.sendall(struct.pack("!I", len(raw)) + raw)


def recv_msg(sock: socket.socket) -> dict | None:
    header = _recv_exact(sock, 4)
    if not header:
        return None
    (length,) = struct.unpack("!I", header)
    raw = _recv_exact(sock, length)
    if not raw:
        return None
    return json.loads(raw.decode("utf-8"))


def _recv_exact(sock: socket.socket, n: int) -> bytes | None:
    buf = bytearray()
    while len(buf) < n:
        chunk = sock.recv(n - len(buf))
        if not chunk:
            return None
        buf.extend(chunk)
    return bytes(buf)


# ─── Main ────────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(description="Playlist Vibe Client")
    parser.add_argument("--host", default=DEFAULT_HOST, help=f"Server host (default: {DEFAULT_HOST})")
    parser.add_argument("--port", type=int, default=DEFAULT_PORT, help=f"Server port (default: {DEFAULT_PORT})")
    parser.add_argument("--vibe", required=True, help='Gewünschte Stimmung, z.B. "Jazz Café", "Party", "Chill"')
    parser.add_argument("--file", help="JSON-Datei mit Songs (optional, sonst Beispiel-Songs)")
    args = parser.parse_args()

    # Songs laden
    if args.file:
        with open(args.file, "r", encoding="utf-8") as f:
            songs = json.load(f)
        print(f"📂 {len(songs)} Songs aus {args.file} geladen")
    else:
        songs = EXAMPLE_SONGS
        print(f"🎵 {len(songs)} Beispiel-Songs verwenden")

    print(f"🎯 Vibe: \"{args.vibe}\"")
    print(f"📡 Verbinde mit {args.host}:{args.port}...")

    # Verbindung aufbauen und Request senden
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    try:
        sock.connect((args.host, args.port))
        print("✅ Verbunden! Sende Request...")

        request = {
            "vibe": args.vibe,
            "songs": songs,
        }
        send_msg(sock, request)
        print("⏳ Warte auf LLM-Antwort (kann etwas dauern)...")

        response = recv_msg(sock)
        if response is None:
            print("❌ Keine Antwort vom Server")
            sys.exit(1)

        if "error" in response:
            print(f"❌ Fehler: {response['error']}")
            sys.exit(1)

        playlist = response.get("playlist", [])
        print(f"\n{'='*60}")
        print(f"🎶 Playlist für \"{response.get('vibe', args.vibe)}\"")
        print(f"   {len(playlist)} von {len(songs)} Songs ausgewählt")
        print(f"{'='*60}")

        for i, song in enumerate(playlist, 1):
            artist = song.get("artist", "???")
            title = song.get("title", "???")
            sid = song.get("id", "")
            id_str = f" (id:{sid})" if sid else ""
            print(f"  {i:2d}. {artist} – {title}{id_str}")

        print()

        # Optional: als JSON speichern
        out_file = f"playlist_{args.vibe.lower().replace(' ', '_')}.json"
        with open(out_file, "w", encoding="utf-8") as f:
            json.dump(response, f, ensure_ascii=False, indent=2)
        print(f"💾 Playlist gespeichert: {out_file}")

    except ConnectionRefusedError:
        print(f"❌ Verbindung zu {args.host}:{args.port} abgelehnt. Läuft der Server?")
        sys.exit(1)
    finally:
        sock.close()


if __name__ == "__main__":
    main()
